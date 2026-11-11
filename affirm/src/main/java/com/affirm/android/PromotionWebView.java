package com.affirm.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.ConnectionException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.PROMO_SCRIPT_MESSAGE_NAME;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.UTF_8;

@SuppressLint("AddJavascriptInterface")
public class PromotionWebView extends AffirmWebView implements AffirmWebChromeClient.Callbacks,
        PromoWebViewClient.Callbacks {

    // Affirm.js throws on an opaque (about:blank) origin and never renders the logo.
    // A file origin still loads android_asset CSS and android_res fonts.
    @VisibleForTesting
    static final String PROMO_BASE_URL = "file:///android_asset/";

    private static final String RENDER_ID = "renderId";

    interface RenderListener {
        void onReady();

        void onFailure(@NonNull String reason);
    }

    private String loadedHtml;
    /** The CSS and typeface of the last promo, which the page was built with. */
    private String loadedCss;
    private String loadedTypeface;
    @Nullable
    private String renderId;
    /** The render the page was loaded with; null when there is no usable page. */
    @Nullable
    private String pageRenderId;
    /** The page has posted for {@code pageRenderId}, so window.affirmPromoRender exists. */
    private boolean pageLoaded;
    @Nullable
    private RenderListener renderListener;
    private boolean shrinkToContentWidth;
    private int contentWidthPx;
    private int lineWidthPx;

    void setRenderListener(@Nullable RenderListener listener) {
        renderListener = listener;
    }

    /**
     * Measures no wider than the promo needs when the parent leaves the width open, like a
     * wrap_content TextView: the one-line width if it fits, else the widest wrapped line. The
     * page reports both once it is ready.
     */
    void setShrinkToContentWidth(boolean shrink) {
        shrinkToContentWidth = shrink;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (shrinkToContentWidth && contentWidthPx > 0 && mode != MeasureSpec.EXACTLY) {
            final int available = MeasureSpec.getSize(widthMeasureSpec);
            int width = contentWidthPx;
            if (mode == MeasureSpec.AT_MOST && width > available) {
                width = lineWidthPx > 0 ? Math.min(lineWidthPx, available) : available;
            }
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setWebViewClickListener(OnClickListener listener) {
        setOnClickListener(listener == null ? null : v -> {
            ViewParent parent = getParent();
            listener.onClick(parent instanceof View ? (View) parent : v);
        });
    }

    public PromotionWebView(Context context) {
        this(context, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public PromotionWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        AffirmUtils.debuggableWebView(getContext());
        getSettings().setSupportMultipleWindows(false);
        getSettings().setAllowFileAccess(false);
        setWebViewClient(new PromoWebViewClient(this));
        setWebChromeClient(new AffirmWebChromeClient(this));
        addJavascriptInterface(new PromoScriptMessageHandler(), PROMO_SCRIPT_MESSAGE_NAME);

        final GestureDetector tapDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapUp(MotionEvent e) {
                        return performClick();
                    }
                });
        tapDetector.setIsLongpressEnabled(false);
        // Consume every touch so the WebView never handles taps itself. A loading promo is
        // transparent but still VISIBLE, so it must not take taps either.
        setOnTouchListener((v, event) -> {
            if (isPromoShown()) {
                tapDetector.onTouchEvent(event);
            }
            return true;
        });
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
        // A hardware layer is a bitmap the list can scroll. The default WebView surface stays
        // blank while a RecyclerView is moving and only composites again once the scroll stops.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** The page has posted ready and the promo is on screen. */
    boolean isPromoShown() {
        return getVisibility() == VISIBLE && getAlpha() > 0f;
    }

    public void loadWebData(String promoHtml, String remoteCssUrl, String typeface) {
        // Merchants may call this again with the same promo, e.g. from onStart. Reloading
        // would hide it until the page is ready again.
        if (isShowingSamePromo(promoHtml, remoteCssUrl, typeface)) {
            return;
        }
        final boolean samePage = pageRenderId != null
                && TextUtils.equals(remoteCssUrl, loadedCss)
                && TextUtils.equals(typeface, loadedTypeface);
        loadedHtml = promoHtml;
        loadedCss = remoteCssUrl;
        loadedTypeface = typeface;
        // VISIBLE, but transparent, until the page reports ready. INVISIBLE marks the document
        // hidden, so requestAnimationFrame does not run while the list is scrolling.
        setVisibility(VISIBLE);
        setAlpha(0f);
        final String id = UUID.randomUUID().toString();
        renderId = id;
        if (samePage) {
            // Reloading would fetch Affirm.js and the stylesheets again, e.g. on every bind
            // of a list row. A loading page renders this once it posts.
            if (pageLoaded) {
                renderInPage(id, promoHtml);
            }
            return;
        }
        final AffirmPromoRendering.FontDeclaration font =
                AffirmPromoRendering.fontDeclaration(typeface);
        if (!AffirmPromoRendering.isPermittedStylesheetUrl(remoteCssUrl)
                || !AffirmPromoRendering.isPermittedStylesheetUrl(font.url)) {
            AffirmLog.e("Promo CSS and font URLs must use https, file:///android_asset/ or "
                    + "file:///android_res/: " + remoteCssUrl + ", " + font.url);
            logFailure(AffirmPromoRenderReason.RESOURCE_FAILED);
            return;
        }
        final String page = initialHtml(id, promoHtml, remoteCssUrl, font);
        if (page == null) {
            logFailure(AffirmPromoRenderReason.TEMPLATE_ERROR);
            return;
        }
        pageRenderId = id;
        pageLoaded = false;
        loadDataWithBaseURL(PROMO_BASE_URL, page, TEXT_HTML, UTF_8, null);
    }

    private void renderInPage(@NonNull String id, String promoHtml) {
        final Map<String, Object> job = new LinkedHashMap<>();
        job.put(RENDER_ID, id);
        job.put("html", nonNull(promoHtml));
        evaluateJavascript("window.affirmPromoRender("
                + AffirmPromoRendering.escapedJsonObject(job) + ");", null);
    }

    /**
     * Hides the promo and ignores the page until the next {@link #loadWebData}, keeping the
     * page loaded for it.
     */
    void hidePromo() {
        renderId = null;
        setVisibility(GONE);
    }

    private boolean isShowingSamePromo(String html, String css, String typeface) {
        return isPromoShown()
                && TextUtils.equals(html, loadedHtml)
                && TextUtils.equals(css, loadedCss)
                && TextUtils.equals(typeface, loadedTypeface);
    }

    @Nullable
    private String initialHtml(@NonNull String id, String promoHtml, String remoteCssUrl,
                               AffirmPromoRendering.FontDeclaration font) {
        final AffirmPlugins plugins = AffirmPlugins.get();
        if (plugins == null) {
            return null;
        }
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(RENDER_ID, id);
        payload.put("html", nonNull(promoHtml));
        payload.put("remoteCssURL", nonNull(remoteCssUrl));
        payload.put("remoteFontURL", font.url);
        payload.put("publicKey", nonNull(plugins.publicKey()));
        payload.put("jsURL", HTTPS_PROTOCOL + plugins.jsUrl() + JS_PATH);
        payload.put("locale", nonNull(plugins.locale()));
        payload.put("countryCode", nonNull(plugins.countryCode()));
        payload.put("bundledCSS", AffirmPromoRendering.bundledCss(getResources()) + font.css);
        payload.put("timeoutMs", AffirmPromoRendering.HTML_TIMEOUT_MS);
        return AffirmPromoRendering.htmlDocumentWithPayload(getResources(), payload);
    }

    @NonNull
    private static String nonNull(@Nullable String value) {
        return value == null ? "" : value;
    }

    private void logFailure(@NonNull String reason) {
        // The page may be what failed, so the next promo loads a new one.
        pageRenderId = null;
        pageLoaded = false;
        hidePromo();
        AffirmLog.e("AffirmPromotionWebView failed: " + reason);
        if (renderListener != null) {
            renderListener.onFailure(reason);
        }
    }

    private void applyContentSize(@NonNull JSONObject body) {
        final float density = getResources().getDisplayMetrics().density;
        final int widthPx = AffirmPromoRendering.heightForCssPixels(body.optInt("width"), density);
        final int linePx =
                AffirmPromoRendering.heightForCssPixels(body.optInt("lineWidth"), density);
        if (widthPx > 0 && (widthPx != contentWidthPx || linePx != lineWidthPx)) {
            contentWidthPx = widthPx;
            lineWidthPx = linePx;
            if (shrinkToContentWidth) {
                requestLayout();
            }
        }
        final int cssHeight = body.optInt("height");
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null || cssHeight <= 0) {
            return;
        }
        int heightPx = AffirmPromoRendering.heightForCssPixels(cssHeight, density);
        if (params.height != heightPx) {
            params.height = heightPx;
            setLayoutParams(params);
        }
    }

    @Override
    public void chromeLoadCompleted() {
        AffirmLog.v("AffirmPromotionWebView has been loaded");
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmLog.e("AffirmPromotionWebView load failed " + error);
        logFailure(AffirmPromoRenderReason.LOAD_ERROR);
    }

    @Override
    public void onRendererGone() {
        logFailure(AffirmPromoRenderReason.RENDER_PROCESS_GONE);
    }

    /** Handles a `window.webkit.messageHandlers.affirmPromo` message from the shared page. */
    @VisibleForTesting
    void onScriptMessage(@Nullable String message) {
        final JSONObject body;
        try {
            body = new JSONObject(message == null ? "" : message);
        } catch (JSONException e) {
            AffirmLog.w("Ignored malformed promo script message");
            return;
        }
        // A page replaced while loading can still post; only the current one counts.
        if (!pageLoaded && pageRenderId != null
                && pageRenderId.equals(body.optString(RENDER_ID))) {
            pageLoaded = true;
            if (renderId != null && !renderId.equals(pageRenderId)) {
                renderInPage(renderId, loadedHtml);
            }
        }
        if (renderId == null || !renderId.equals(body.optString(RENDER_ID))) {
            AffirmLog.v("Ignored promo script message from a previous render");
            return;
        }
        switch (body.optString("event")) {
            case "ready":
                applyContentSize(body);
                setAlpha(1f);
                setVisibility(VISIBLE);
                if (renderListener != null) {
                    renderListener.onReady();
                }
                break;
            case "height":
                applyContentSize(body);
                break;
            case "failure":
                logFailure(AffirmPromoRenderReason.fromPage(body.optString("reason", null)));
                break;
            default:
                break;
        }
    }

    @VisibleForTesting
    @Nullable
    String renderId() {
        return renderId;
    }

    @SuppressWarnings("unused")
    private class PromoScriptMessageHandler {
        @JavascriptInterface
        public void postMessage(final String message) {
            post(() -> onScriptMessage(message));
        }
    }
}

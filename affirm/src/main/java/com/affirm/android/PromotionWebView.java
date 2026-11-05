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

    interface RenderListener {
        void onReady();

        void onFailure(@NonNull String reason);
    }

    private String loadedHtml;
    private String loadedCss;
    private String loadedTypeface;
    @Nullable
    private String renderId;
    @Nullable
    private RenderListener renderListener;

    void setRenderListener(@Nullable RenderListener listener) {
        renderListener = listener;
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
        // Consume every touch so the WebView never handles taps itself.
        setOnTouchListener((v, event) -> {
            tapDetector.onTouchEvent(event);
            return true;
        });
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    public void loadWebData(String promoHtml, String remoteCssUrl, String typeface) {
        // Merchants may call this again with the same promo, e.g. from onStart. Reloading
        // would hide it until the page is ready again.
        if (isShowingSamePromo(promoHtml, remoteCssUrl, typeface)) {
            return;
        }
        loadedHtml = promoHtml;
        loadedCss = remoteCssUrl;
        loadedTypeface = typeface;
        setVisibility(INVISIBLE);
        final String id = UUID.randomUUID().toString();
        renderId = id;
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
        loadDataWithBaseURL(PROMO_BASE_URL, page, TEXT_HTML, UTF_8, null);
    }

    private boolean isShowingSamePromo(String html, String css, String typeface) {
        return getVisibility() == VISIBLE
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
        payload.put("renderId", id);
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
        renderId = null;
        loadedHtml = null;
        loadedCss = null;
        loadedTypeface = null;
        setVisibility(GONE);
        AffirmLog.e("AffirmPromotionWebView failed: " + reason);
        if (renderListener != null) {
            renderListener.onFailure(reason);
        }
    }

    private void applyContentHeight(int cssHeight) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null || cssHeight <= 0) {
            return;
        }
        int heightPx = AffirmPromoRendering.heightForCssPixels(cssHeight,
                getResources().getDisplayMetrics().density);
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
        if (renderId == null || !renderId.equals(body.optString("renderId"))) {
            AffirmLog.v("Ignored promo script message from a previous render");
            return;
        }
        switch (body.optString("event")) {
            case "ready":
                applyContentHeight(body.optInt("height"));
                setVisibility(VISIBLE);
                if (renderListener != null) {
                    renderListener.onReady();
                }
                break;
            case "height":
                applyContentHeight(body.optInt("height"));
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

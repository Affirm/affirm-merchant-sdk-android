package com.affirm.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.AFFIRM_FONT;
import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.BUNDLED_CSS;
import static com.affirm.android.AffirmConstants.COUNTRY_CODE;
import static com.affirm.android.AffirmConstants.HTML_FRAGMENT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.LOCALE;
import static com.affirm.android.AffirmConstants.PROMO_JS_BRIDGE;
import static com.affirm.android.AffirmConstants.REMOTE_CSS_URL;
import static com.affirm.android.AffirmConstants.SANITIZER_JS;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.UTF_8;

@SuppressLint("AddJavascriptInterface")
public class PromotionWebView extends AffirmWebView implements AffirmWebChromeClient.Callbacks,
        PromoWebViewClient.Callbacks {

    // Affirm.js throws on an opaque (about:blank) origin and never renders the logo.
    // A file origin still loads android_asset CSS and android_res fonts.
    @VisibleForTesting
    static final String PROMO_BASE_URL = "file:///android_asset/";

    private String loadedHtml;
    private String loadedCss;
    private String loadedTypeface;

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
        setWebViewClient(new PromoWebViewClient(this));
        setWebChromeClient(new AffirmWebChromeClient(this));
        addJavascriptInterface(new PromoJsBridge(), PROMO_JS_BRIDGE);

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
        if (!shouldReload(getVisibility(), promoHtml, remoteCssUrl, typeface,
                loadedHtml, loadedCss, loadedTypeface)) {
            return;
        }
        loadedHtml = promoHtml;
        loadedCss = remoteCssUrl;
        loadedTypeface = typeface;
        setVisibility(INVISIBLE);
        if (!isAllowedCssUrl(remoteCssUrl)) {
            AffirmLog.e("Remote promo CSS must use https or file:///android_asset/: "
                    + remoteCssUrl);
            logFailure(PromoRenderReason.RESOURCE_FAILED);
            return;
        }
        loadDataWithBaseURL(PROMO_BASE_URL, initialHtml(promoHtml, remoteCssUrl, typeface),
                TEXT_HTML, UTF_8, null);
    }

    @VisibleForTesting
    static boolean shouldReload(int visibility, String html, String css, String typeface,
                                String loadedHtml, String loadedCss, String loadedTypeface) {
        return visibility != VISIBLE
                || !TextUtils.equals(html, loadedHtml)
                || !TextUtils.equals(css, loadedCss)
                || !TextUtils.equals(typeface, loadedTypeface);
    }

    private static boolean isAllowedCssUrl(@Nullable String remoteCssUrl) {
        return TextUtils.isEmpty(remoteCssUrl)
                || URLUtil.isHttpsUrl(remoteCssUrl)
                || URLUtil.isAssetUrl(remoteCssUrl);
    }

    private String initialHtml(String promoHtml, String remoteCssUrl, String typeface) {
        String html;
        InputStream ins = null;
        try {
            ins = getResources().openRawResource(R.raw.affirm_promo);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            AffirmUtils.closeInputStream(ins);
        }

        final HashMap<String, String> map = new HashMap<>();
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().jsUrl() + JS_PATH;

        String bundledCss = readRaw(R.raw.badge_styles);

        map.put(SANITIZER_JS, readRaw(R.raw.dompurify));
        map.put(AFFIRM_FONT, encodeForPage(typeface));
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(LOCALE, AffirmPlugins.get().locale());
        map.put(COUNTRY_CODE, AffirmPlugins.get().countryCode());
        map.put(HTML_FRAGMENT, encodeForPage(promoHtml));
        map.put(REMOTE_CSS_URL, encodeForPage(remoteCssUrl));
        map.put(BUNDLED_CSS, bundledCss);
        return AffirmUtils.replacePlaceholders(html, map);
    }

    /** Base64 of UTF-8; the page decodes it, so markup never reaches the template directly. */
    @VisibleForTesting
    @NonNull
    static String encodeForPage(@Nullable String value) {
        return TextUtils.isEmpty(value) ? ""
                : Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    @NonNull
    private String readRaw(int resId) {
        InputStream ins = null;
        try {
            ins = getResources().openRawResource(resId);
            return AffirmUtils.readInputStream(ins);
        } catch (Resources.NotFoundException | IOException e) {
            AffirmLog.e("Failed to load raw resource "
                    + getResources().getResourceEntryName(resId) + ": " + e);
            return "";
        } finally {
            AffirmUtils.closeInputStream(ins);
        }
    }

    private void logFailure(@NonNull String reason) {
        loadedHtml = null;
        loadedCss = null;
        loadedTypeface = null;
        setVisibility(GONE);
        AffirmLog.e("AffirmPromotionWebView failed: " + reason);
    }

    void applyContentHeight(int cssHeight) {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params == null || cssHeight <= 0) {
            return;
        }
        int heightPx = cssToDevicePx(cssHeight, getResources().getDisplayMetrics().density);
        if (params.height != heightPx) {
            params.height = heightPx;
            setLayoutParams(params);
        }
    }

    /** The WebView maps CSS px 1:1 to dp. */
    @VisibleForTesting
    static int cssToDevicePx(int cssPx, float density) {
        return (int) Math.ceil(cssPx * density);
    }

    @Override
    public void chromeLoadCompleted() {
        AffirmLog.v("AffirmPromotionWebView has been loaded");
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmLog.e("AffirmPromotionWebView load failed " + error);
        logFailure(PromoRenderReason.LOAD_ERROR);
    }

    @Override
    public void onRendererGone() {
        logFailure(PromoRenderReason.RENDER_PROCESS_GONE);
    }

    @SuppressWarnings("unused")
    private class PromoJsBridge {
        @JavascriptInterface
        public void onSanitized(final boolean usable) {
            AffirmLog.v("AffirmPromotionWebView sanitized, usable=" + usable);
        }

        @JavascriptInterface
        public void onReady(final int cssHeight) {
            post(() -> {
                applyContentHeight(cssHeight);
                setVisibility(VISIBLE);
            });
        }

        @JavascriptInterface
        public void onHeightChanged(final int cssHeight) {
            post(() -> applyContentHeight(cssHeight));
        }

        @JavascriptInterface
        public void onFailure(final String reason) {
            post(() -> logFailure(PromoRenderReason.fromPage(reason)));
        }
    }
}

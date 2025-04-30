package com.affirm.android;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.affirm.android.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.AFFIRM_FONT;
import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.COUNTRY_CODE;
import static com.affirm.android.AffirmConstants.HTML_FRAGMENT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.LOCALE;
import static com.affirm.android.AffirmConstants.REMOTE_CSS_URL;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.UTF_8;

public class PromotionWebView extends AffirmWebView implements AffirmWebChromeClient.Callbacks,
        AffirmWebViewClient.WebViewClientCallbacks {

    private OnClickListener webViewClickListener;

    public void setWebViewClickListener(OnClickListener webViewClickListener) {
        this.webViewClickListener = webViewClickListener;
    }

    public PromotionWebView(Context context) {
        this(context, null);
    }

    public PromotionWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        AffirmUtils.debuggableWebView(getContext());
        setWebViewClient(new PromoWebViewClient(this));
        setWebChromeClient(new AffirmWebChromeClient(this));
        GestureDetector gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        performClick();
                        return true;
                    }
                });
        setOnTouchListener((view, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }
            return false;
        });
        setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (webViewClickListener != null) {
            webViewClickListener.onClick((View) getParent());
        }
        return true;
    }

    public void loadWebData(String promoHtml, String remoteCssUrl, String typeface) {
        final String html = initialHtml(promoHtml, remoteCssUrl, typeface);
        loadDataWithBaseURL(null, html, TEXT_HTML, UTF_8, null);
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

        map.put(AFFIRM_FONT, typeface != null ? typeface : "");
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(LOCALE, AffirmPlugins.get().locale());
        map.put(COUNTRY_CODE, AffirmPlugins.get().countryCode());
        map.put(HTML_FRAGMENT, promoHtml);
        map.put(REMOTE_CSS_URL, remoteCssUrl != null ? remoteCssUrl : "");
        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void chromeLoadCompleted() {
        AffirmLog.v("AffirmPromotionWebView has been loaded");
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmLog.e("AffirmPromotionWebView load failed" + error.toString());
    }
}
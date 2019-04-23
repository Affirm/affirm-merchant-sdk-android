package com.affirm.android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

final class AffirmWebView extends WebView {
    private static final String USER_AGENT_PREFIX = "Affirm-SDK:Android-"
            + BuildConfig.VERSION_NAME;

    @SuppressLint("SetJavaScriptEnabled")
    public AffirmWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final String userAgent = USER_AGENT_PREFIX + " " + getSettings().getUserAgentString();
        getSettings().setUserAgentString(userAgent);
        clearCache(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setSupportMultipleWindows(true);
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        setVerticalScrollBarEnabled(false);
    }
}
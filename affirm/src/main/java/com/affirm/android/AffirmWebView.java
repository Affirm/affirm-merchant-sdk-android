package com.affirm.android;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

import com.affirm.android.BuildConfig;

final class AffirmWebView extends WebView {
    private static final String USER_AGENT_PREFIX = "Affirm-SDK:Android-" + BuildConfig.VERSION_NAME;

    public AffirmWebView(Context context) {
        this(context, null);
    }

    public AffirmWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final String userAgent = USER_AGENT_PREFIX + " " + getSettings().getUserAgentString();
        getSettings().setUserAgentString(userAgent);
        clearCache(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setSupportMultipleWindows(true);
        setVerticalScrollBarEnabled(false);
    }
}
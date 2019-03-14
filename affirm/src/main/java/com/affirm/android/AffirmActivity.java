package com.affirm.android;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

abstract class AffirmActivity extends AppCompatActivity implements AffirmWebChromeClient.Callbacks {
    static final String PROTOCOL = "https://";

    ViewGroup container;
    WebView webView;
    View progressIndicator;

    abstract void initViews();

    abstract void beforeOnCreate();

    abstract void initData(@Nullable Bundle savedInstanceState);

    abstract void onAttached();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        beforeOnCreate();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_webview);
        container = findViewById(R.id.container);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        initViews();

        initData(savedInstanceState);

        onAttached();
    }

    @Override
    protected void onDestroy() {
        clearCookies();
        container.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }

    public void clearCookies() {
        final CookieManager cookieManager = CookieManager.getInstance();
        final CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
        CookiesUtil.clearCookieByUrl("https://" + AffirmPlugins.get().baseUrl(),
                cookieManager, cookieSyncManager);
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }
}

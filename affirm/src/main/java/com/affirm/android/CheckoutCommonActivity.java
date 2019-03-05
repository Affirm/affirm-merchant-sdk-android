package com.affirm.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

abstract class CheckoutCommonActivity extends AppCompatActivity implements AffirmWebViewClient.Callbacks, AffirmWebChromeClient.Callbacks {

    public static final int RESULT_ERROR = -8575;

    public static final String CHECKOUT_ERROR = "checkout_error";

    static final String CHECKOUT_EXTRA = "checkout_extra";

    Checkout checkout;

    WebView webView;
    View progressIndicator;

    abstract void startCheckout();

    abstract void setupWebView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.hideActionBar(this);

        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }

        setContentView(R.layout.activity_webview);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        setupWebView();

        startCheckout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
    }

    @Override
    protected void onDestroy() {
        clearCookies();
        webView.destroy();
        super.onDestroy();
    }

    public void clearCookies() {
        final CookieManager cookieManager = CookieManager.getInstance();
        final CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
        CookiesUtil.clearCookieByUrl("https://" + AffirmPlugins.get().baseUrl(), cookieManager, cookieSyncManager);
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
    }

    @Override
    public void onWebViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onWebViewPageLoaded() {
        // ignore this
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }

    public interface CheckoutCallback {

        void onError(Exception exception);

        void onSuccess(CheckoutResponse response);
    }

    static class CheckoutResponseWrapper {

        CheckoutResponse response;

        Exception exception;

        CheckoutResponseWrapper(CheckoutResponse checkoutResponse, Exception exception) {
            this.response = checkoutResponse;
            this.exception = exception;
        }
    }
}

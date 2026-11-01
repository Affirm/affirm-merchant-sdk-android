package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

final class PromoWebViewClient extends AffirmWebViewClient {

    interface Callbacks extends WebViewClientCallbacks {
        void onRendererGone();
    }

    @NonNull
    private final Callbacks promoCallbacks;

    PromoWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.promoCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return true;
    }

    @Override
    void notifyRenderProcessGone(@NonNull String message) {
        promoCallbacks.onRendererGone();
    }
}

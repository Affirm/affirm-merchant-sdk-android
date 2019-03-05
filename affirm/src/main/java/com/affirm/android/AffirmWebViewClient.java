package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

public abstract class AffirmWebViewClient extends WebViewClient {
    static final String AFFIRM_CONFIRMATION_URL = "affirm://checkout/confirmed";
    static final String AFFIRM_CANCELLATION_URL = "affirm://checkout/cancelled";

    private final Callbacks callbacks;

    AffirmWebViewClient(@NonNull Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        callbacks.onWebViewPageLoaded();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.contains(AFFIRM_CANCELLATION_URL)) {
            callbacks.onWebViewCancellation();
            return true;
        }

        if (hasCallbackUrl(view, url)) {
            return true;
        }

        return !url.startsWith("http");
    }

    abstract boolean hasCallbackUrl(WebView view, String url);

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        callbacks.onWebViewError(new Exception(error.toString()));
    }

    public interface Callbacks {
        void onWebViewError(@NonNull Throwable error);

        void onWebViewCancellation();

        void onWebViewPageLoaded();
    }
}

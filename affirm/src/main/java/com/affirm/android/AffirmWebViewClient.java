package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

public abstract class AffirmWebViewClient extends WebViewClient {
    public static final String AFFIRM_CONFIRMATION_URL = "affirm://checkout/confirmed";
    public static final String AFFIRM_CANCELLATION_URL = "affirm://checkout/cancelled";

    private final Callbacks callbacks;

    abstract boolean hasCallbackUrl(WebView view, String url);

    AffirmWebViewClient(@NonNull Callbacks callbacks) {
        this.callbacks = callbacks;
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

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        callbacks.onWebViewError(new Exception(error.toString()));
    }

    public interface Callbacks {
        void onWebViewError(@NonNull Throwable error);

        void onWebViewCancellation();
    }
}

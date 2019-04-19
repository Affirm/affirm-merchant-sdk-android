package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.affirm.android.exception.ConnectionException;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.HTTP;

abstract class AffirmWebViewClient extends WebViewClient {

    private final WebViewClientCallbacks callbacks;

    abstract boolean hasCallbackUrl(WebView view, String url);

    AffirmWebViewClient(@NonNull WebViewClientCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (hasCallbackUrl(view, url)) {
            return true;
        }

        return !url.startsWith(HTTP);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        callbacks.onWebViewError(new ConnectionException(error.toString()));
    }

    public interface WebViewClientCallbacks {
        void onWebViewError(@NonNull ConnectionException error);
    }
}

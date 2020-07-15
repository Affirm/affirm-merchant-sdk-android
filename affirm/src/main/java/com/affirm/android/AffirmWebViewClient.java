package com.affirm.android;

import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.affirm.android.exception.ConnectionException;

import static com.affirm.android.AffirmConstants.HTTP;

abstract class AffirmWebViewClient extends WebViewClient {

    private final WebViewClientCallbacks callbacks;

    abstract boolean hasCallbackUrl(WebView view, String url);

    AffirmWebViewClient(@NonNull WebViewClientCallbacks callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            if (hasCallbackUrl(view, url)) {
                return true;
            }
        } catch (Exception e) {
            AffirmLog.e("Override url failed: " + e.toString());
        }
        return !url.startsWith(HTTP);
    }

    // This method was deprecated in API level 23
    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description,
                                String failingUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return;
        }
        callbacks.onWebViewError(new ConnectionException(errorCode + ", " + description));
    }

    @TargetApi(android.os.Build.VERSION_CODES.M)
    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        // Please be aware that the new SDK 23 callback will be called for any resource
        // (iframe, image, etc) that failed to load, not just for the main page
        if (request.isForMainFrame()) {
            callbacks.onWebViewError(new ConnectionException(
                    error.getErrorCode() + ", " + error.getDescription().toString()));
        }
    }

    public interface WebViewClientCallbacks {
        void onWebViewError(@NonNull ConnectionException error);
    }
}

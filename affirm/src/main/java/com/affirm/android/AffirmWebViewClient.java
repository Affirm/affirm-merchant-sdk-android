package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.affirm.android.exception.ConnectionException;

import androidx.annotation.NonNull;

import static com.affirm.android.Constants.AFFIRM_CANCELLATION_URL;
import static com.affirm.android.Constants.HTTP;

abstract class AffirmWebViewClient extends WebViewClient {

    private final Callbacks mCallbacks;

    abstract boolean hasCallbackUrl(WebView view, String url);

    AffirmWebViewClient(@NonNull Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.contains(AFFIRM_CANCELLATION_URL)) {
            mCallbacks.onWebViewCancellation();
            return true;
        }

        if (hasCallbackUrl(view, url)) {
            return true;
        }

        return !url.startsWith(HTTP);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        mCallbacks.onWebViewError(new ConnectionException(error.toString()));
    }

    public interface Callbacks {
        void onWebViewError(@NonNull ConnectionException error);

        void onWebViewCancellation();
    }
}

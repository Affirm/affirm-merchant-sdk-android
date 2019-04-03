package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

final class TrackWebViewClient extends AffirmWebViewClient {

    private final Callbacks mCallbacks;

    TrackWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
    }
}

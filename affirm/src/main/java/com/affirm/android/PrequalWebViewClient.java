package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.REFERRING_URL;

final class PrequalWebViewClient extends AffirmWebViewClient {

    private final Callbacks callbacks;

    PrequalWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.equals(REFERRING_URL)) {
            callbacks.onWebViewConfirmation();
            return true;
        }
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation();
    }
}

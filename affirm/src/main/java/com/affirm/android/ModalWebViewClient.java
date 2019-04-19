package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;

final class ModalWebViewClient extends AffirmWebViewClient {

    private final Callbacks callbacks;

    ModalWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            callbacks.onWebViewCancellation();
            return true;
        }
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewCancellation();
    }
}

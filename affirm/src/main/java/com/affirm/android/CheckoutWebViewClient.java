package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

final class CheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks callbacks;

    CheckoutWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CONFIRMATION_URL)) {
            final String token = url.split("checkout_token=")[1];
            callbacks.onWebViewConfirmation(token);
            return true;
        } else if (url.contains(AFFIRM_CANCELLATION_URL)) {
            callbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    interface Callbacks extends AffirmWebViewClient.Callbacks {
        void onWebViewConfirmation(@NonNull String token);
    }
}

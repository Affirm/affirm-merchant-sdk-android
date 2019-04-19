package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_TOKEN;

final class CheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks callbacks;

    CheckoutWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CHECKOUT_CONFIRMATION_URL)) {
            final String token = url.split(CHECKOUT_TOKEN + "=")[1];
            callbacks.onWebViewConfirmation(token);
            return true;
        } else if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            callbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation(@NonNull String token);

        void onWebViewCancellation();
    }
}

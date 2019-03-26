package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.Constants.AFFIRM_CANCELLATION_URL;
import static com.affirm.android.Constants.AFFIRM_CONFIRMATION_URL;
import static com.affirm.android.Constants.CHECKOUT_TOKEN;

final class CheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks mCallbacks;

    CheckoutWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CONFIRMATION_URL)) {
            final String token = url.split(CHECKOUT_TOKEN + "=")[1];
            mCallbacks.onWebViewConfirmation(token);
            return true;
        } else if (url.contains(AFFIRM_CANCELLATION_URL)) {
            mCallbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation(@NonNull String token);
    }
}

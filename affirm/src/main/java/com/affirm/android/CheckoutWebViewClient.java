package com.affirm.android;

import android.net.Uri;
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
            final String token = Uri.parse(url).getQueryParameter(CHECKOUT_TOKEN);
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

package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.Constants.AFFIRM_CHECKOUT_CANCELLATION_URL;

final class ModalWebViewClient extends AffirmWebViewClient {

    private final Callbacks mCallbacks;

    ModalWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            mCallbacks.onWebViewCancellation();
            return true;
        }
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewCancellation();
    }
}

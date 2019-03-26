package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.Constants.REFERRING_URL;

final class PrequalWebViewClient extends AffirmWebViewClient {

    private final Callbacks mCallbacks;

    PrequalWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.equals(REFERRING_URL)) {
            mCallbacks.onWebViewConfirmation();
            return true;
        }
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation();
    }
}

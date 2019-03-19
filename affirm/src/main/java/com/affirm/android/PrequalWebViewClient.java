package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

final class PrequalWebViewClient extends AffirmWebViewClient {

    static final String REFERRING_URL = "https://androidsdk/";

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

    interface Callbacks extends AffirmWebViewClient.Callbacks {
        void onWebViewConfirmation();
    }
}

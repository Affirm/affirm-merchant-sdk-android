package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

final class ModalWebViewClient extends AffirmWebViewClient {

    ModalWebViewClient(@NonNull WebViewClientCallbacks callbacks) {
        super(callbacks);
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        return false;
    }
}

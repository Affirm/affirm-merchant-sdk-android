package com.affirm.android;

import android.webkit.WebView;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.INLINE_LEARN_MORE_CLICK_URL;

final class InlineWebViewClient extends AffirmWebViewClient {

    private final InlineWebViewClient.Callbacks callbacks;

    InlineWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(INLINE_LEARN_MORE_CLICK_URL)) {
            callbacks.onLearnMoreClicked();
            return true;
        }
        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onLearnMoreClicked();
    }
}

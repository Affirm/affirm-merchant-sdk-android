package com.affirm.android;

import android.webkit.WebView;

import com.affirm.android.model.CardDetails;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import androidx.annotation.NonNull;

final class VcnCheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks mCallbacks;

    VcnCheckoutWebViewClient(@NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CONFIRMATION_URL)) {
            final String encodedString = url.split("data=")[1];
            try {
                final String json = URLDecoder.decode(encodedString, "UTF-8");
                Gson gson = AffirmPlugins.get().gson();
                final CardDetails cardDetails = gson.fromJson(json, CardDetails.class);
                mCallbacks.onWebViewConfirmation(cardDetails);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else if (url.contains(AFFIRM_CANCELLATION_URL)) {
            mCallbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    interface Callbacks extends AffirmWebViewClient.Callbacks {
        void onWebViewConfirmation(@NonNull CardDetails cardDetails);
    }
}

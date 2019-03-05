package com.affirm.android;

import android.webkit.WebView;

import com.affirm.android.model.CardDetails;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import androidx.annotation.NonNull;

public final class VcnCheckoutWebViewClient extends AffirmWebViewClient {
    private final Gson gson;
    private final Callbacks callbacks;

    public VcnCheckoutWebViewClient(@NonNull Gson gson, @NonNull Callbacks callbacks) {
        super(callbacks);
        this.gson = gson;
        this.callbacks = callbacks;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CONFIRMATION_URL)) {
            final String encodedString = url.split("data=")[1];
            try {
                final String json = URLDecoder.decode(encodedString, "UTF-8");
                final CardDetails cardDetails = gson.fromJson(json, CardDetails.class);
                callbacks.onWebViewConfirmation(cardDetails);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else if (url.contains(AFFIRM_CANCELLATION_URL)) {
            callbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    public interface Callbacks extends AffirmWebViewClient.Callbacks {
        void onWebViewConfirmation(@NonNull CardDetails cardDetails);
    }
}

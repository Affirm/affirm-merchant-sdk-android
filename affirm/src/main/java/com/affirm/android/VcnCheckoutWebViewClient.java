package com.affirm.android;

import android.webkit.WebView;

import com.affirm.android.model.CardDetails;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import androidx.annotation.NonNull;

import static com.affirm.android.Constants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.Constants.AFFIRM_CHECKOUT_CONFIRMATION_URL;

final class VcnCheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks mCallbacks;
    private final Gson mGson;
    private static final String VCN_CHECKOUT_REGEX = "data=";
    private static final String ENCODING_FORMAT = "UTF-8";

    VcnCheckoutWebViewClient(@NonNull Gson gson, @NonNull Callbacks callbacks) {
        super(callbacks);
        mCallbacks = callbacks;
        mGson = gson;
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CHECKOUT_CONFIRMATION_URL)) {
            final String encodedString = url.split(VCN_CHECKOUT_REGEX)[1];
            try {
                final String json = URLDecoder.decode(encodedString, ENCODING_FORMAT);
                final CardDetails cardDetails = mGson.fromJson(json, CardDetails.class);
                mCallbacks.onWebViewConfirmation(cardDetails);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            mCallbacks.onWebViewCancellation();
            return true;
        }

        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation(@NonNull CardDetails cardDetails);

        void onWebViewCancellation();
    }
}

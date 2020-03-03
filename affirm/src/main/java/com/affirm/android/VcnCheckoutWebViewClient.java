package com.affirm.android;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.VcnReason;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;

final class VcnCheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks callbacks;
    private final Gson gson;
    private final String receiveReasonCodes;
    private static final String VCN_CHECKOUT_REGEX = "data=";
    private static final String ENCODING_FORMAT = "UTF-8";
    private static final String INVALID_CHECKOUT_REDIRECT_URL =
            "https://sandbox.affirm.com/u/";
    private static final String INVALID_CHECKOUT_MESSAGE =
            "Checkout status is in an invalid state.";

    VcnCheckoutWebViewClient(@NonNull Gson gson, @NonNull String receiveReasonCodes,
                             @NonNull Callbacks callbacks) {
        super(callbacks);
        this.receiveReasonCodes = receiveReasonCodes;
        this.callbacks = callbacks;
        this.gson = gson;
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request.getUrl().toString().equals(INVALID_CHECKOUT_REDIRECT_URL)) {
            callbacks.onWebViewError(
                    new ConnectionException(INVALID_CHECKOUT_MESSAGE)
            );
            return null;
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    boolean hasCallbackUrl(WebView view, String url) {
        if (url.contains(AFFIRM_CHECKOUT_CONFIRMATION_URL)) {
            final String encodedString = url.split(VCN_CHECKOUT_REGEX)[1];
            try {
                final String json = URLDecoder.decode(encodedString, ENCODING_FORMAT);
                final CardDetails cardDetails = gson.fromJson(json, CardDetails.class);
                callbacks.onWebViewConfirmation(cardDetails);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            final String encodedString = url.split(VCN_CHECKOUT_REGEX)[1];
            try {
                final String json = URLDecoder.decode(encodedString, ENCODING_FORMAT);
                final VcnReason vcnReason = gson.fromJson(json, VcnReason.class);

                if (receiveReasonCodes.equals("false")) {
                    callbacks.onWebViewCancellation();
                } else {
                    callbacks.onWebViewCancellationReason(vcnReason);
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return true;
        }

        return false;
    }

    interface Callbacks extends WebViewClientCallbacks {
        void onWebViewConfirmation(@NonNull CardDetails cardDetails);

        void onWebViewCancellationReason(@NonNull VcnReason vcnReason);

        void onWebViewCancellation();
    }
}

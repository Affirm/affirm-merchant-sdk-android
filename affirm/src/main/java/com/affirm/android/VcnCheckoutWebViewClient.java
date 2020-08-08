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
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.INVALID_CHECKOUT_MESSAGE;

final class VcnCheckoutWebViewClient extends AffirmWebViewClient {
    private final Callbacks callbacks;
    private final Gson gson;
    private final String receiveReasonCodes;
    private static final String VCN_CHECKOUT_REGEX = "data=";
    private static final String ENCODING_FORMAT = "UTF-8";

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
        if (request.getUrl().toString().equals(HTTPS_PROTOCOL
                + AffirmPlugins.get().baseInvalidCheckoutRedirectUrl())) {
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
            AffirmLog.d("VCN checkout confirmed: " + encodedString);
            try {
                final String json = URLDecoder.decode(encodedString, ENCODING_FORMAT);
                final CardDetails cardDetails = gson.fromJson(json, CardDetails.class);
                callbacks.onWebViewConfirmation(cardDetails);
            } catch (UnsupportedEncodingException | JsonSyntaxException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else if (url.contains(AFFIRM_CHECKOUT_CANCELLATION_URL)) {
            final String encodedString = url.split(VCN_CHECKOUT_REGEX)[1];
            AffirmLog.d("VCN checkout cancelled: " + encodedString);
            try {
                final String json = URLDecoder.decode(encodedString, ENCODING_FORMAT);
                final VcnReason vcnReason = gson.fromJson(json, VcnReason.class);

                if (receiveReasonCodes.equals("false")) {
                    callbacks.onWebViewCancellation();
                } else {
                    callbacks.onWebViewCancellationReason(vcnReason);
                }
            } catch (UnsupportedEncodingException | JsonSyntaxException e) {
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

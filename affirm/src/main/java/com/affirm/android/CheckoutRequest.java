package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import okhttp3.Call;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.API_VERSION_KEY;
import static com.affirm.android.AffirmConstants.API_VERSION_VALUE;
import static com.affirm.android.AffirmConstants.CHECKOUT;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.MERCHANT;
import static com.affirm.android.AffirmConstants.METADATA;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_VALUE;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_VALUE;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_KEY;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_VALUE;

class CheckoutRequest implements AffirmRequest {

    @NonNull
    private final Checkout checkout;
    private final boolean useVCN;
    @Nullable
    private final InnerCheckoutCallback checkoutCallback;
    @Nullable
    private final String caas;
    private final int cardAuthWindow;

    private Call checkoutCall;

    private final JsonParser jsonParser = new JsonParser();
    private final Gson gson = AffirmPlugins.get().gson();

    CheckoutRequest(@NonNull Checkout checkout, @Nullable InnerCheckoutCallback callback,
                    @Nullable String caas, boolean useVCN, int cardAuthWindow) {
        this.checkout = checkout;
        this.checkoutCallback = callback;
        this.caas = caas;
        this.useVCN = useVCN;
        this.cardAuthWindow = cardAuthWindow;
    }

    @Override
    public void create() {
        if (checkoutCall != null) {
            checkoutCall.cancel();
        }
        checkoutCall = AffirmClient.send(new AffirmCheckoutRequest(),
                new AffirmClient.AffirmListener<CheckoutResponse>() {
            @Override
            public void onSuccess(CheckoutResponse response) {
                if (checkoutCallback != null) {
                    checkoutCallback.onSuccess(response);
                }
            }

            @Override
            public void onFailure(AffirmException exception) {
                handleErrorResponse(exception);
            }
        });
    }

    @Override
    public void cancel() {
        if (checkoutCall != null) {
            checkoutCall.cancel();
            checkoutCall = null;
        }
    }

    private void handleErrorResponse(@NonNull AffirmException e) {
        AffirmLog.e(e.toString());

        if (checkoutCallback != null) {
            checkoutCallback.onError(e);
        }
    }

    private JsonObject parseToJsonObject(Object object) {
        return jsonParser.parse(gson.toJson(object)).getAsJsonObject();
    }


    class AffirmCheckoutRequest implements AffirmClient.AffirmApiRequest {

        @NotNull
        @Override
        public String url() {
            return AffirmHttpClient.getProtocol()
                    + AffirmPlugins.get().baseUrl()
                    + CHECKOUT_PATH;
        }

        @NotNull
        @Override
        public AffirmHttpRequest.Method method() {
            return AffirmHttpRequest.Method.POST;
        }

        @Nullable
        @Override
        public JsonObject body() {
            Merchant merchant;
            Integer authWindow = cardAuthWindow >= 0 ? cardAuthWindow : null;
            if (useVCN) {
                merchant = Merchant.builder()
                        .setPublicApiKey(AffirmPlugins.get().publicKey())
                        .setUseVcn(true)
                        .setName(AffirmPlugins.get().merchantName())
                        .setCaas(caas)
                        .setCardAuthWindow(authWindow)
                        .build();
            } else {
                merchant = Merchant.builder()
                        .setPublicApiKey(AffirmPlugins.get().publicKey())
                        .setConfirmationUrl(AFFIRM_CHECKOUT_CONFIRMATION_URL)
                        .setCancelUrl(AFFIRM_CHECKOUT_CANCELLATION_URL)
                        .setName(AffirmPlugins.get().merchantName())
                        .setCaas(caas)
                        .setCardAuthWindow(authWindow)
                        .build();
            }

            final JsonObject merchantJson = parseToJsonObject(merchant);
            merchantJson.addProperty(USER_CONFIRMATION_URL_ACTION_KEY,
                    USER_CONFIRMATION_URL_ACTION_VALUE);

            final JsonObject checkoutJson = parseToJsonObject(checkout);
            checkoutJson.add(MERCHANT, merchantJson);
            checkoutJson.addProperty(API_VERSION_KEY, API_VERSION_VALUE);

            // Need to set `platform_type` & `platform_affirm` by default
            JsonObject metadataJson = checkoutJson.getAsJsonObject(METADATA);
            if (metadataJson == null) {
                metadataJson = new JsonObject();
            }
            metadataJson.addProperty(PLATFORM_TYPE_KEY, PLATFORM_TYPE_VALUE);
            metadataJson.addProperty(PLATFORM_AFFIRM_KEY, PLATFORM_AFFIRM_VALUE);

            final JsonObject jsonRequest = new JsonObject();
            jsonRequest.add(CHECKOUT, checkoutJson);
            return jsonRequest;
        }
    }
}

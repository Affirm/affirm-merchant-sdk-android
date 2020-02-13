package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.affirm.android.model.Shipping;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.ADDRESS;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.API_VERSION_KEY;
import static com.affirm.android.AffirmConstants.API_VERSION_VALUE;
import static com.affirm.android.AffirmConstants.CHECKOUT;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.MERCHANT;
import static com.affirm.android.AffirmConstants.METADATA;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_VALUE;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_VALUE;
import static com.affirm.android.AffirmConstants.SHIPPING;
import static com.affirm.android.AffirmConstants.TAG_CHECKOUT;
import static com.affirm.android.AffirmConstants.TAG_VCN_CHECKOUT;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_KEY;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_VALUE;
import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;

class CheckoutRequest implements AffirmRequest {

    @NonNull
    private final Checkout checkout;
    private final boolean useVCN;
    @Nullable
    private final InnerCheckoutCallback checkoutCallback;

    private Call checkoutCall;

    CheckoutRequest(@NonNull Checkout checkout,
                    @Nullable InnerCheckoutCallback callback,
                    boolean useVCN) {
        this.checkout = checkout;
        this.checkoutCallback = callback;
        this.useVCN = useVCN;
    }

    @Override
    public void create() {
        Merchant merchant;

        if (useVCN) {
            merchant = Merchant.builder()
                    .setPublicApiKey(AffirmPlugins.get().publicKey())
                    .setUseVcn(true)
                    .setName(AffirmPlugins.get().merchantName())
                    .build();
        } else {
            merchant = Merchant.builder()
                    .setPublicApiKey(AffirmPlugins.get().publicKey())
                    .setConfirmationUrl(AFFIRM_CHECKOUT_CONFIRMATION_URL)
                    .setCancelUrl(AFFIRM_CHECKOUT_CANCELLATION_URL)
                    .setName(AffirmPlugins.get().merchantName())
                    .build();
        }

        Gson gson = AffirmPlugins.get().gson();
        final JsonParser jsonParser = new JsonParser();

        final JsonObject merchantJson = jsonParser.parse(gson.toJson(merchant)).getAsJsonObject();
        merchantJson
                .addProperty(USER_CONFIRMATION_URL_ACTION_KEY, USER_CONFIRMATION_URL_ACTION_VALUE);
        final JsonObject metadataJson =
                jsonParser.parse(gson.toJson(checkout.metadata())).getAsJsonObject();
        metadataJson.addProperty(PLATFORM_TYPE_KEY, PLATFORM_TYPE_VALUE);
        metadataJson.addProperty(PLATFORM_AFFIRM_KEY, PLATFORM_AFFIRM_VALUE);

        final JsonObject checkoutJson = jsonParser.parse(gson.toJson(checkout)).getAsJsonObject();
        final Shipping shipping = checkout.shippingAddress();
        if (shipping != null) {
            JsonObject address = jsonParser
                    .parse(gson.toJson(shipping.address()))
                    .getAsJsonObject();
            ((JsonObject) checkoutJson.get(SHIPPING)).add(ADDRESS, address);
        }
        checkoutJson.add(MERCHANT, merchantJson);
        checkoutJson.addProperty(API_VERSION_KEY, API_VERSION_VALUE);
        checkoutJson.add(METADATA, metadataJson);

        final JsonObject jsonRequest = new JsonObject();
        jsonRequest.add(CHECKOUT, checkoutJson);

        if (checkoutCall != null) {
            checkoutCall.cancel();
        }

        checkoutCall = AffirmPlugins.get().restClient().getCallForRequest(
                new AffirmHttpRequest.Builder()
                        .setUrl(
                                AffirmHttpClient.getProtocol()
                                        + AffirmPlugins.get().baseUrl()
                                        + CHECKOUT_PATH
                        )
                        .setMethod(AffirmHttpRequest.Method.POST)
                        .setBody(new AffirmHttpBody(CONTENT_TYPE, jsonRequest.toString()))
                        .setTag(useVCN ? TAG_VCN_CHECKOUT : TAG_CHECKOUT)
                        .build()
        );
        checkoutCall.enqueue(new Callback() {
            @Override
            public void onResponse(
                    @NotNull Call call,
                    @NotNull Response response
            ) throws IOException {
                ResponseBody responseBody = response.body();

                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        CheckoutResponse checkoutResponse = gson.fromJson(
                                responseBody.string(),
                                CheckoutResponse.class
                        );

                        if (checkoutCallback != null) {
                            new Handler(Looper.getMainLooper()).post(
                                    () -> checkoutCallback.onSuccess(checkoutResponse)
                            );
                        }
                    } else {
                        handleErrorResponse(new APIException("i/o failure", null));
                    }
                } else {
                    AffirmException affirmException =
                            AffirmHttpClient.createExceptionAndTrackFromResponse(
                                    call.request(),
                                    response,
                                    responseBody
                            );

                    handleErrorResponse(affirmException);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                AffirmTracker.track(
                        NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(
                                call.request(),
                                null
                        )
                );
                handleErrorResponse(new ConnectionException("i/o failure", e));
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
            new Handler(Looper.getMainLooper()).post(() -> checkoutCallback.onError(e));
        }
    }
}

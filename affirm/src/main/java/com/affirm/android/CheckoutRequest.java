package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.AffirmError;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.TAG_CHECKOUT;
import static com.affirm.android.AffirmConstants.TAG_VCN_CHECKOUT;
import static com.affirm.android.AffirmConstants.X_AFFIRM_REQUEST_ID;
import static com.affirm.android.AffirmHttpClient.handleAPIError;
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
        final JsonObject metadataJson = new JsonObject();

        merchantJson.addProperty("user_confirmation_url_action", "GET");
        metadataJson.addProperty("platform_type", "Affirm Android SDK");
        metadataJson.addProperty("platform_affirm", BuildConfig.VERSION_NAME);

        final JsonObject checkoutJson = jsonParser.parse(gson.toJson(checkout)).getAsJsonObject();

        checkoutJson.add("merchant", merchantJson);
        checkoutJson.addProperty("api_version", "v2");
        checkoutJson.add("metadata", metadataJson);

        final JsonObject jsonRequest = new JsonObject();
        jsonRequest.add("checkout", checkoutJson);

        AffirmHttpRequest checkoutRequest = new AffirmHttpRequest.Builder()
                .setUrl(AffirmApiHandler.getProtocol() + AffirmPlugins.get().baseUrl() + CHECKOUT_PATH)
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody(CONTENT_TYPE, jsonRequest.toString()))
                .setTag(useVCN ? TAG_VCN_CHECKOUT : TAG_CHECKOUT)
                .build();

        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();
        Request okHttpRequest = httpClient.getRequest(checkoutRequest);

        if (checkoutCall != null) {
            checkoutCall.cancel();
        }

        checkoutCall = httpClient.getOkHttpClientt().newCall(okHttpRequest);
        checkoutCall.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        CheckoutResponse checkoutResponse = gson.fromJson(responseBody.string(), CheckoutResponse.class);

                        if (checkoutCallback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> checkoutCallback.onSuccess(checkoutResponse));
                        }
                    } else {
                        handleErrorResponse(new ConnectionException("i/o failure"));
                    }
                } else {
                    AffirmTracker.track(NETWORK_ERROR, ERROR, createTrackingNetworkJsonObj(okHttpRequest, response));
                    if (responseBody != null && responseBody.contentLength() > 0) {
                        final AffirmError affirmError = AffirmPlugins.get().gson().fromJson(responseBody.charStream(), AffirmError.class);
                        AffirmException affirmException = handleAPIError(affirmError, response.code(), response.headers().get(X_AFFIRM_REQUEST_ID));
                        handleErrorResponse(affirmException);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                AffirmTracker.track(NETWORK_ERROR, ERROR, createTrackingNetworkJsonObj(okHttpRequest, null));
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

    private void handleErrorResponse(AffirmException e) {
        AffirmLog.e(e.toString());

        if (checkoutCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> checkoutCallback.onError(e));
        }
    }
}

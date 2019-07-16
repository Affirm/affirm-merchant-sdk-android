package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.AffirmError;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.HTTP;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.PROMO_PATH;
import static com.affirm.android.AffirmConstants.TAG_CHECKOUT;
import static com.affirm.android.AffirmConstants.TAG_GET_NEW_PROMO;
import static com.affirm.android.AffirmConstants.TAG_TRACKER;
import static com.affirm.android.AffirmConstants.TAG_VCN_CHECKOUT;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;
import static com.affirm.android.AffirmConstants.X_AFFIRM_REQUEST_ID;
import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;


final class AffirmApiHandler {

    interface ApiCallback {
        void onSuccess(@NonNull PromoResponse response);
        void onError(@NonNull IOException e);
    }

    private AffirmApiHandler() {
    }

    static void cancelNewPromoCall() {
        AffirmPlugins.get().restClient().cancelCallWithTag(TAG_GET_NEW_PROMO);
    }

    static void fetchPromo(
            @Nullable String promoId,
            @Nullable PromoPageType pageType,
            float dollarAmount,
            boolean showCta,
            @NonNull String logoColor,
            @NonNull String logoType,
            @NonNull ApiCallback callback
    ) {
        int centAmount = AffirmUtils.decimalDollarsToIntegerCents(dollarAmount);

        StringBuilder path = new StringBuilder(
                String.format(
                        Locale.getDefault(),
                        PROMO_PATH,
                        AffirmPlugins.get().publicKey(),
                        centAmount,
                        showCta
                )
        );

        if (promoId != null) {
            path.append("&promo_external_id=").append(promoId);
        }

        if (pageType != null) {
            path.append("&page_type=").append(pageType.getType());
        }

        path.append("&logo_color=").append(logoColor).append("&logo_type=").append(logoType);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getProtocol() + AffirmPlugins.get().baseUrl() + path.toString())
                .setMethod(AffirmHttpRequest.Method.GET)
                .setTag(TAG_GET_NEW_PROMO)
                .build();

        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();
        Request okHttpRequest = httpClient.getRequest(request);

        httpClient.getOkHttpClientt()
                .newCall(okHttpRequest)
                .enqueue(new Callback() {
                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Gson gson = AffirmPlugins.get().gson();
                            ResponseBody responseBody = response.body();

                            final Headers headers = response.headers();
                            String requestId = headers.get(X_AFFIRM_REQUEST_ID);

                            if (response.code() < 200 || response.code() >= 300) {
                                if (responseBody != null && responseBody.contentLength() > 0) {
                                    final AffirmError affirmError = gson.fromJson(responseBody.charStream(), AffirmError.class);

                                    try {
                                        AffirmHttpClient.handleAPIError(affirmError, response.code(), requestId);
                                    } catch (APIException e) {
                                        e.printStackTrace();
                                    } catch (PermissionException e) {
                                        e.printStackTrace();
                                    } catch (InvalidRequestException e) {
                                        e.printStackTrace();
                                    }

                                    callback.onError(new IOException(affirmError.message()));
                                }
                            } else {
                                if (responseBody != null) {
                                    PromoResponse promoResponse = gson.fromJson(responseBody.string(), PromoResponse.class);
                                    callback.onSuccess(promoResponse);
                                } else {
                                    callback.onError(new IOException("Response was success, but body was null"));
                                }
                            }
                        } else {
                            AffirmTracker.track(NETWORK_ERROR, ERROR, createTrackingNetworkJsonObj(okHttpRequest, response));
                            callback.onError(new IOException("Response was not successful"));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        AffirmTracker.track(NETWORK_ERROR, ERROR, createTrackingNetworkJsonObj(okHttpRequest, null));
                        callback.onError(e);
                    }
                });
    }

    static void cancelVcnCheckoutCall() {
        AffirmPlugins.get().restClient().cancelCallWithTag(TAG_VCN_CHECKOUT);
    }

    static CheckoutResponse executeVcnCheckout(Checkout checkout) throws APIException,
            PermissionException, InvalidRequestException, ConnectionException {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();

        final Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setUseVcn(true)
                .setName(AffirmPlugins.get().merchantName())
                .build();

        final JsonObject jsonRequest = buildCheckoutJsonRequest(checkout, merchant);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getProtocol() + AffirmPlugins.get().baseUrl() + CHECKOUT_PATH)
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody(CONTENT_TYPE, jsonRequest.toString()))
                .setTag(TAG_VCN_CHECKOUT)
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), CheckoutResponse.class);
    }

    static void cancelCheckoutCall() {
        AffirmPlugins.get().restClient().cancelCallWithTag(TAG_CHECKOUT);
    }

    static CheckoutResponse executeCheckout(Checkout checkout) throws APIException,
            PermissionException, InvalidRequestException, ConnectionException {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();

        final Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setConfirmationUrl(AFFIRM_CHECKOUT_CONFIRMATION_URL)
                .setCancelUrl(AFFIRM_CHECKOUT_CANCELLATION_URL)
                .setName(AffirmPlugins.get().merchantName())
                .build();

        final JsonObject jsonRequest = buildCheckoutJsonRequest(checkout, merchant);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getProtocol() + AffirmPlugins.get().baseUrl() + CHECKOUT_PATH)
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody(CONTENT_TYPE, jsonRequest.toString()))
                .setTag(TAG_CHECKOUT)
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), CheckoutResponse.class);
    }

    static void sendTrackRequest(JsonObject trackData) throws APIException,
            PermissionException, InvalidRequestException, ConnectionException {
        AffirmPlugins plugins = AffirmPlugins.get();
        AffirmHttpClient httpClient = plugins.restClient();

        final AffirmHttpBody body = new AffirmHttpBody(CONTENT_TYPE, trackData.toString());
        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getTrackerProtocol() + plugins.trackerBaseUrl() + TRACKER_PATH)
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(body)
                .setTag(TAG_TRACKER)
                .build();

        httpClient.execute(request, false);
    }

    private static String getTrackerProtocol() {
        return AffirmPlugins.get().trackerBaseUrl().contains(HTTP) ? "" : HTTPS_PROTOCOL;
    }

    private static String getProtocol() {
        return AffirmPlugins.get().baseUrl().contains(HTTP) ? "" : HTTPS_PROTOCOL;
    }

    private static JsonObject buildCheckoutJsonRequest(Checkout checkout, Merchant merchant) {
        final JsonObject metadataJson = new JsonObject();
        final JsonObject jsonRequest = new JsonObject();
        final JsonParser jsonParser = new JsonParser();

        final JsonObject checkoutJson =
                jsonParser.parse(AffirmPlugins.get().gson().toJson(checkout)).getAsJsonObject();
        final JsonObject merchantJson =
                jsonParser.parse(AffirmPlugins.get().gson().toJson(merchant)).getAsJsonObject();

        merchantJson.addProperty("user_confirmation_url_action", "GET");
        metadataJson.addProperty("platform_type", "Affirm Android SDK");
        metadataJson.addProperty("platform_affirm", BuildConfig.VERSION_NAME);

        checkoutJson.add("merchant", merchantJson);
        checkoutJson.addProperty("api_version", "v2");
        checkoutJson.add("metadata", metadataJson);

        jsonRequest.add("checkout", checkoutJson);

        return jsonRequest;
    }
}

package com.affirm.android;


import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.HTTP;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.TAG_CHECKOUT;
import static com.affirm.android.AffirmConstants.TAG_GET_NEW_PROMO;
import static com.affirm.android.AffirmConstants.TAG_TRACKER;
import static com.affirm.android.AffirmConstants.TAG_VCN_CHECKOUT;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;

final class AffirmApiHandler {
    private AffirmApiHandler() {
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

    static String getProtocol() {
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

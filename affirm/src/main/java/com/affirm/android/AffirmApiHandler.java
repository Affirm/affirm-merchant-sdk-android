package com.affirm.android;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.affirm.android.model.PromoResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

import androidx.annotation.Nullable;

final class AffirmApiHandler {

    private AffirmApiHandler() {
    }

    private static final String CHECKOUT_PATH = "/api/v2/checkout/";
    private static final String TRACKER_PATH = "/collect";

    private static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private static final String TAG_GET_NEW_PROMO = "GET_NEW_PROMO";
    private static final String TAG_CHECKOUT = "CHECKOUT";
    private static final String TAG_VCN_CHECKOUT = "VCN_CHECKOUT";
    private static final String TAG_TRACKER = "TAG_TRACKER";

    static void cancelNewPromoCall() {
        AffirmPlugins.get().restClient().cancelCallWithTag(TAG_GET_NEW_PROMO);
    }

    static PromoResponse getNewPromo(@Nullable String promoId, float dollarAmount, boolean showCta)
            throws APIException, PermissionException, InvalidRequestException, ConnectionException {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();
        int centAmount = AffirmUtils.decimalDollarsToIntegerCents(dollarAmount);
        String path = String.format(Locale.getDefault(),
                "/api/promos/v2/%s?is_sdk=true"
                        + "&field=ala"
                        + "&amount=%d"
                        + "&show_cta=%s"
                        + "&promo_external_id=%s",
                AffirmPlugins.get().publicKey(), centAmount, showCta, promoId);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getProtocol() + AffirmPlugins.get().baseUrl() + path)
                .setMethod(AffirmHttpRequest.Method.GET)
                .setTag(TAG_GET_NEW_PROMO)
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), PromoResponse.class);
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
                .setName(AffirmPlugins.get().name())
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
                .setConfirmationUrl(AffirmWebViewClient.AFFIRM_CONFIRMATION_URL)
                .setCancelUrl(AffirmWebViewClient.AFFIRM_CANCELLATION_URL)
                .setName(AffirmPlugins.get().name())
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
        return AffirmPlugins.get().trackerBaseUrl().contains("http") ? "" : "https://";
    }

    private static String getProtocol() {
        return AffirmPlugins.get().baseUrl().contains("http") ? "" : "https://";
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

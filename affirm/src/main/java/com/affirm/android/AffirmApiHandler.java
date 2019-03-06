package com.affirm.android;

import android.text.TextUtils;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.affirm.android.model.PromoResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

class AffirmApiHandler {

    private static final String CHECKOUT_PATH = "/api/v2/checkout/";

    static PromoResponse getNewPromo(String promoId, float dollarAmount, boolean showCta) throws IOException {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();
        int centAmount = AffirmUtils.decimalDollarsToIntegerCents(dollarAmount);
        String path;
        if (TextUtils.isEmpty(promoId)) {
            path = String.format(Locale.getDefault(),
                    "/api/promos/v2/%s?is_sdk=true&field=ala&amount=%d&show_cta=%s",
                    AffirmPlugins.get().publicKey(), centAmount, showCta);
        } else {
            path = String.format(Locale.getDefault(),
                    "/api/promos/v2/%s?is_sdk=true&field=ala&amount=%d&show_cta=%s&promo_external_id=%s",
                    AffirmPlugins.get().publicKey(), centAmount, showCta, promoId);
        }

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(getProtocol() + AffirmPlugins.get().baseUrl() + path)
                .setMethod(AffirmHttpRequest.Method.GET)
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), PromoResponse.class);
    }

    static CheckoutResponse executeVcnCheckout(Checkout checkout) throws IOException {
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
                .setBody(new AffirmHttpBody("application/json; charset=utf-8", jsonRequest.toString()))
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), CheckoutResponse.class);
    }

    static CheckoutResponse executeCheckout(Checkout checkout) throws IOException {
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
                .setBody(new AffirmHttpBody("application/json; charset=utf-8", jsonRequest.toString()))
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), CheckoutResponse.class);
    }

    private static String getProtocol() {
        return AffirmPlugins.get().baseUrl().contains("http") ? "" : "https://";
    }

    private static JsonObject buildCheckoutJsonRequest(Checkout checkout, Merchant merchant) {
        final JsonObject metadataJson = new JsonObject();
        final JsonObject jsonRequest = new JsonObject();
        final JsonParser jsonParser = new JsonParser();

        final JsonObject checkoutJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(checkout)).getAsJsonObject();
        final JsonObject merchantJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(merchant)).getAsJsonObject();

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

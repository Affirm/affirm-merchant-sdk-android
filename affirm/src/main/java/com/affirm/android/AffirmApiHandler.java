package com.affirm.android;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

class AffirmApiHandler {

    static CheckoutResponse executeVcnCheckout(Checkout checkout) throws IOException {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();

        final Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setUseVcn(true)
                .setName(AffirmPlugins.get().name())
                .build();

        final JsonObject jsonRequest = buildCheckoutJsonRequest(checkout, merchant);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl("https://sandbox.affirm.com/api/v2/checkout/")
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
                .setUrl("https://sandbox.affirm.com/api/v2/checkout/")
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody("application/json; charset=utf-8", jsonRequest.toString()))
                .build();

        AffirmHttpResponse response = httpClient.execute(request);
        return AffirmPlugins.get().gson().fromJson(response.getContent(), CheckoutResponse.class);
    }

    private static JsonObject buildCheckoutJsonRequest(Checkout checkout, Merchant merchant) {
        final JsonObject configJson = new JsonObject();
        final JsonObject metadataJson = new JsonObject();
        final JsonObject jsonRequest = new JsonObject();
        final JsonParser jsonParser = new JsonParser();

        final JsonObject checkoutJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(checkout)).getAsJsonObject();
        final JsonObject merchantJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(merchant)).getAsJsonObject();

        configJson.addProperty("user_confirmation_url_action", "GET");
        metadataJson.addProperty("platform_type", "Affirm Android SDK");
        metadataJson.addProperty("platform_affirm", BuildConfig.VERSION_NAME);

        checkoutJson.add("merchant", merchantJson);
        checkoutJson.add("config", configJson);
        checkoutJson.addProperty("api_version", "v2");
        checkoutJson.add("metadata", metadataJson);

        jsonRequest.add("checkout", checkoutJson);

        return jsonRequest;
    }
}

package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.joda.money.Money;

import okhttp3.Call;
import okhttp3.OkHttpClient;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.API_VERSION_KEY;
import static com.affirm.android.AffirmConstants.API_VERSION_VALUE;
import static com.affirm.android.AffirmConstants.CHECKOUT;
import static com.affirm.android.AffirmConstants.CHECKOUT_BFF_PATH;
import static com.affirm.android.AffirmConstants.CHECKOUT_HEADER_AFFIRM_LOCALE;
import static com.affirm.android.AffirmConstants.CHECKOUT_HEADER_COUNTRY_CODE;
import static com.affirm.android.AffirmConstants.CHECKOUT_META;
import static com.affirm.android.AffirmConstants.CHECKOUT_META_LOCALE;
import static com.affirm.android.AffirmConstants.CHECKOUT_MODE;
import static com.affirm.android.AffirmConstants.CHECKOUT_PATH;
import static com.affirm.android.AffirmConstants.COUNTRY_CODE_KEY;
import static com.affirm.android.AffirmConstants.IS_EXPRESS_CHECKOUT;
import static com.affirm.android.AffirmConstants.MERCHANT;
import static com.affirm.android.AffirmConstants.METADATA;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_AFFIRM_VALUE;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_KEY;
import static com.affirm.android.AffirmConstants.PLATFORM_TYPE_VALUE;
import static com.affirm.android.AffirmConstants.SHIPPING;
import static com.affirm.android.AffirmConstants.SHIPPING_AMOUNT;
import static com.affirm.android.AffirmConstants.SHIPPING_AND_TOTALS_CALLBACK_URL;
import static com.affirm.android.AffirmConstants.TAX_AMOUNT;
import static com.affirm.android.AffirmConstants.TOTAL;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_KEY;
import static com.affirm.android.AffirmConstants.USER_CONFIRMATION_URL_ACTION_VALUE;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_START;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_START;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

import java.util.HashMap;
import java.util.Map;

class CheckoutRequest implements AffirmRequest {

    @Nullable
    private final OkHttpClient okHttpClient;
    @NonNull
    private final Checkout checkout;
    @Nullable
    private final Money money;
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
                    @Nullable String caas, @Nullable Money money, boolean useVCN,
                    int cardAuthWindow) {
        this(null, checkout, callback, caas, money, useVCN, cardAuthWindow);
    }

    @VisibleForTesting
    CheckoutRequest(@Nullable OkHttpClient okHttpClient, @NonNull Checkout checkout,
                    @Nullable InnerCheckoutCallback callback, @Nullable String caas,
                    @Nullable Money money, boolean useVCN, int cardAuthWindow) {
        this.okHttpClient = okHttpClient;
        this.checkout = checkout;
        this.money = money;
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

        AffirmCheckoutRequest request = new AffirmCheckoutRequest();

        // Track checkout info
        trackCheckout(request);

        checkoutCall = AffirmClient.send(okHttpClient, request,
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
        AffirmLog.e("Checkout request failed", e);

        if (checkoutCallback != null) {
            checkoutCallback.onError(e);
        }
    }

    private JsonObject parseToJsonObject(Object object) {
        return jsonParser.parse(gson.toJson(object)).getAsJsonObject();
    }

    private boolean isExpress() {
        return checkout.checkoutMode() == Checkout.CheckoutMode.EXPRESS;
    }

    private void trackCheckout(AffirmCheckoutRequest request) {
        JsonObject trackInfo = request.body();
        if (trackInfo == null) {
            trackInfo = new JsonObject();
        }
        trackInfo.addProperty("useVCN", useVCN);
        if (useVCN) {
            AffirmTracker.track(VCN_CHECKOUT_CREATION_START, INFO, trackInfo);
        } else {
            AffirmTracker.track(CHECKOUT_WEBVIEW_START, INFO, trackInfo);
        }
    }

    private Merchant merchant() {
        final Merchant.Builder builder = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setName(AffirmPlugins.get().merchantName())
                .setCaas(caas)
                .setCardAuthWindow(cardAuthWindow >= 0 ? cardAuthWindow : null)
                .setShippingAndTotalsCallbackUrl(
                        isExpress() ? checkout.shippingAndTotalsCallbackUrl() : null);
        if (useVCN) {
            builder.setUseVcn(true);
        } else {
            builder.setConfirmationUrl(AFFIRM_CHECKOUT_CONFIRMATION_URL)
                    .setCancelUrl(AFFIRM_CHECKOUT_CANCELLATION_URL);
        }
        return builder.build();
    }

    private JsonObject checkoutPayload() {
        final JsonObject merchantJson = parseToJsonObject(merchant());
        merchantJson.addProperty(USER_CONFIRMATION_URL_ACTION_KEY,
                USER_CONFIRMATION_URL_ACTION_VALUE);

        final JsonObject checkoutJson = parseToJsonObject(checkout);
        // SDK-only fields; the callback URL is sent on the merchant instead.
        checkoutJson.remove(CHECKOUT_MODE);
        checkoutJson.remove(SHIPPING_AND_TOTALS_CALLBACK_URL);
        checkoutJson.add(MERCHANT, merchantJson);
        checkoutJson.addProperty(API_VERSION_KEY, API_VERSION_VALUE);

        final JsonObject metaJson = new JsonObject();
        metaJson.addProperty(CHECKOUT_META_LOCALE, AffirmPlugins.get().locale());
        checkoutJson.add(CHECKOUT_META, metaJson);

        // Need to set `platform_type` & `platform_affirm` by default
        JsonObject metadataJson = checkoutJson.getAsJsonObject(METADATA);
        if (metadataJson == null) {
            metadataJson = new JsonObject();
            checkoutJson.add(METADATA, metadataJson);
        }
        metadataJson.addProperty(PLATFORM_TYPE_KEY, PLATFORM_TYPE_VALUE);
        metadataJson.addProperty(PLATFORM_AFFIRM_KEY, PLATFORM_AFFIRM_VALUE);
        return checkoutJson;
    }

    private JsonObject standardBody() {
        final JsonObject checkoutJson = checkoutPayload();
        if (money != null) {
            checkoutJson.addProperty(TOTAL,
                    AffirmUtils.decimalDollarsToIntegerCents(money.getAmount()));
        }
        final JsonObject body = new JsonObject();
        body.add(CHECKOUT, checkoutJson);
        return body;
    }

    /**
     * checkout-bff: the payload at the top level. Affirm collects the shipping address and gets
     * shipping, tax and the total from the merchant's Shipping &amp; Totals endpoint, so none of
     * them are sent.
     */
    private JsonObject expressBody() {
        final JsonObject checkoutJson = checkoutPayload();
        checkoutJson.remove(SHIPPING);
        checkoutJson.remove(TOTAL);
        checkoutJson.remove(SHIPPING_AMOUNT);
        checkoutJson.remove(TAX_AMOUNT);
        checkoutJson.getAsJsonObject(CHECKOUT_META).addProperty(IS_EXPRESS_CHECKOUT, true);
        checkoutJson.addProperty(COUNTRY_CODE_KEY, AffirmPlugins.get().countryCode());
        return checkoutJson;
    }

    class AffirmCheckoutRequest implements AffirmClient.AffirmApiRequest {

        @NotNull
        @Override
        public String url() {
            return AffirmHttpClient.getProtocol()
                    + AffirmPlugins.get().checkoutUrl()
                    + (isExpress() ? CHECKOUT_BFF_PATH : CHECKOUT_PATH);
        }

        @NotNull
        @Override
        public AffirmHttpRequest.Method method() {
            return AffirmHttpRequest.Method.POST;
        }

        @Nullable
        @Override
        public JsonObject body() {
            return isExpress() ? expressBody() : standardBody();
        }

        @Nullable
        @Override
        public Map<String, String> headers() {
            HashMap<String, String> headers = new HashMap<>();
            headers.put(CHECKOUT_HEADER_AFFIRM_LOCALE, AffirmPlugins.get().locale());
            headers.put(CHECKOUT_HEADER_COUNTRY_CODE, AffirmPlugins.get().countryCode());
            return headers;
        }
    }
}

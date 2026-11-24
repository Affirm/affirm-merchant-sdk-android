package com.affirm.android;

import com.affirm.android.model.Checkout;
import com.google.common.truth.Truth;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import okhttp3.Request;
import okhttp3.Call;
import okhttp3.OkHttpClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;

@RunWith(RobolectricTestRunner.class)
public class CheckoutRequestTest {

    private static final String expectedCheckoutBody =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\",\"meta\":{\"locale\":\"en_US\"}}}";

    private static final String expectedCheckoutWithCaasBody =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"caas\":\"4626b631-c5bc-4c4e-800b-dd5fa27ef8b8\",\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\",\"meta\":{\"locale\":\"en_US\"}}}";

    private static final String expectedCheckoutWithCardAuthWindowBody =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"card_auth_window\":10,\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\",\"meta\":{\"locale\":\"en_US\"}}}";

    private static final String expectedCheckoutWithOutMetadata =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\",\"meta\":{\"locale\":\"en_US\"},\"metadata\":{\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"\"}}}";

    private static String generateExpectedCheckoutBody() {
        return String.format(expectedCheckoutBody, BuildConfig.VERSION_NAME);
    }

    private static String generateExpectedCheckoutWithCaasBody() {
        return String.format(expectedCheckoutWithCaasBody, BuildConfig.VERSION_NAME);
    }

    private static String generateExpectedCheckoutWithCardAuthWindowBody() {
        return String.format(expectedCheckoutWithCardAuthWindowBody, BuildConfig.VERSION_NAME);
    }

    private static String generateExpectedCheckoutWithOutMetadata() {
        return String.format(expectedCheckoutWithOutMetadata, BuildConfig.VERSION_NAME);
    }

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build());
        }
    }

    @Test
    public void testCheckout() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        Checkout checkout = CheckoutFactory.create();
        CheckoutRequest checkoutRequest = new CheckoutRequest(client, checkout, null, null, null, false, -1);
        checkoutRequest.create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Truth.assertThat(RequestUtilsTest.bodyToString(request)).isEqualTo(generateExpectedCheckoutBody());
    }

    @Test
    public void testCheckoutWithCaas() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        Checkout checkout = CheckoutFactory.create();
        CheckoutRequest checkoutRequest = new CheckoutRequest(client, checkout, null, "4626b631-c5bc-4c4e-800b-dd5fa27ef8b8", null, false, -1);
        checkoutRequest.create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Truth.assertThat(RequestUtilsTest.bodyToString(request)).isEqualTo(generateExpectedCheckoutWithCaasBody());
    }

    @Test
    public void testCheckoutWithCardAuthWindow() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        Checkout checkout = CheckoutFactory.create();
        CheckoutRequest checkoutRequest = new CheckoutRequest(client, checkout, null, null, null, false, 10);
        checkoutRequest.create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Truth.assertThat(RequestUtilsTest.bodyToString(request)).isEqualTo(generateExpectedCheckoutWithCardAuthWindowBody());
    }

    @Test
    public void testCheckoutWithOutMetadata() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        Checkout checkout = CheckoutFactory.createWithoutMetadata();
        CheckoutRequest checkoutRequest = new CheckoutRequest(client, checkout, null, null, null, false, -1);
        checkoutRequest.create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Truth.assertThat(RequestUtilsTest.bodyToString(request)).isEqualTo(generateExpectedCheckoutWithOutMetadata());
    }

    @Test
    public void testStandardCheckoutUsesLegacyEndpoint() throws Exception {
        Request request = send(CheckoutFactory.create(), null);

        Truth.assertThat(request.url().encodedPath()).isEqualTo("/api/v2/checkout/");
    }

    @Test
    public void testStandardCheckoutTotalFromMoney() throws Exception {
        Request request = send(CheckoutFactory.create(), Money.of(CurrencyUnit.USD, 500));

        JsonObject checkout = new JsonParser().parse(RequestUtilsTest.bodyToString(request))
                .getAsJsonObject().getAsJsonObject("checkout");
        Truth.assertThat(checkout.get("total").getAsInt()).isEqualTo(50000);
    }

    @Test
    public void testVcnCheckout() throws Exception {
        Request request = send(CheckoutFactory.create(), null, true);

        Truth.assertThat(request.url().encodedPath()).isEqualTo("/api/v2/checkout/");
        JsonObject merchant = new JsonParser().parse(RequestUtilsTest.bodyToString(request))
                .getAsJsonObject().getAsJsonObject("checkout").getAsJsonObject("merchant");
        Truth.assertThat(merchant.get("use_vcn").getAsBoolean()).isTrue();
        Truth.assertThat(merchant.has("user_confirmation_url")).isFalse();
        Truth.assertThat(merchant.has("user_cancel_url")).isFalse();
        Truth.assertThat(merchant.get("user_confirmation_url_action").getAsString()).isEqualTo("GET");
    }

    @Test
    public void testStandardCheckoutIgnoresCallbackUrl() throws Exception {
        Checkout checkout = Checkout.builder()
                .setItems(CheckoutFactory.expressItems())
                .setShipping(CheckoutFactory.create().shippingAddress())
                .setShippingAndTotalsCallbackUrl("https://merchant.com/shipping-and-totals")
                .setTotal(BigDecimal.valueOf(1100.00))
                .setShippingAmount(BigDecimal.valueOf(1000.00))
                .setTaxAmount(BigDecimal.valueOf(100.00))
                .build();

        Request request = send(checkout, null);

        Truth.assertThat(request.url().encodedPath()).isEqualTo("/api/v2/checkout/");
        Truth.assertThat(RequestUtilsTest.bodyToString(request))
                .doesNotContain("merchant_shipping_and_totals_callback_url");
    }

    @Test
    public void testExpressCheckout() throws Exception {
        Request request = send(CheckoutFactory.createExpress(), null);

        Truth.assertThat(request.url().toString())
                .isEqualTo("https://api.global-sandbox.affirm.com/api/checkout-bff/v1/create-checkout");
        Truth.assertThat(request.header("Affirm-Locale")).isEqualTo("en_US");
        Truth.assertThat(request.header("Country-Code")).isEqualTo("USA");

        JsonObject body = new JsonParser().parse(RequestUtilsTest.bodyToString(request)).getAsJsonObject();
        Truth.assertThat(body.has("checkout")).isFalse();
        Truth.assertThat(body.get("order_id").getAsString()).isEqualTo("order-123");
        Truth.assertThat(body.has("items")).isTrue();
        Truth.assertThat(body.get("country_code").getAsString()).isEqualTo("USA");
        Truth.assertThat(body.get("api_version").getAsString()).isEqualTo("v2");

        Truth.assertThat(body.has("total")).isFalse();
        Truth.assertThat(body.has("shipping_amount")).isFalse();
        Truth.assertThat(body.has("tax_amount")).isFalse();
        Truth.assertThat(body.has("shipping")).isFalse();
        Truth.assertThat(body.has("checkoutMode")).isFalse();
        Truth.assertThat(body.has("merchant_shipping_and_totals_callback_url")).isFalse();

        JsonObject meta = body.getAsJsonObject("meta");
        Truth.assertThat(meta.get("is_express_checkout").getAsBoolean()).isTrue();
        Truth.assertThat(meta.get("locale").getAsString()).isEqualTo("en_US");

        JsonObject merchant = body.getAsJsonObject("merchant");
        Truth.assertThat(merchant.get("merchant_shipping_and_totals_callback_url").getAsString())
                .isEqualTo("https://merchant.com/shipping-and-totals");
        Truth.assertThat(merchant.get("user_confirmation_url").getAsString())
                .isEqualTo("affirm://checkout/confirmed");
        Truth.assertThat(merchant.get("user_cancel_url").getAsString())
                .isEqualTo("affirm://checkout/cancelled");

        JsonObject metadata = body.getAsJsonObject("metadata");
        Truth.assertThat(metadata.get("subtotal").getAsString()).isEqualTo("100000");
        Truth.assertThat(metadata.get("platform_type").getAsString()).isEqualTo("Affirm Android SDK");
    }

    @Test
    public void testExpressCheckoutDropsShipping() throws Exception {
        Checkout standard = CheckoutFactory.create();
        Checkout checkout = Checkout.builder()
                .setCheckoutMode(Checkout.CheckoutMode.EXPRESS)
                .setItems(CheckoutFactory.expressItems())
                .setOrderId("order-123")
                .setShippingAndTotalsCallbackUrl("https://merchant.com/shipping-and-totals")
                .setMetadata(CheckoutFactory.expressMetadata())
                .setShipping(standard.shippingAddress())
                .setTotal(BigDecimal.ZERO)
                .setShippingAmount(BigDecimal.ZERO)
                .setTaxAmount(BigDecimal.ZERO)
                .build();

        Request request = send(checkout, null);

        JsonObject body = new JsonParser().parse(RequestUtilsTest.bodyToString(request)).getAsJsonObject();
        Truth.assertThat(body.has("shipping")).isFalse();
    }

    @Test
    public void testExpressCheckoutDropsTotalFromMoney() throws Exception {
        Request request = send(CheckoutFactory.createExpress(), Money.of(CurrencyUnit.USD, 500));

        JsonObject body = new JsonParser().parse(RequestUtilsTest.bodyToString(request)).getAsJsonObject();
        Truth.assertThat(body.has("total")).isFalse();
    }

    private Request send(Checkout checkout, Money money) {
        return send(checkout, money, false);
    }

    private Request send(Checkout checkout, Money money, boolean useVCN) {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        new CheckoutRequest(client, checkout, null, null, money, useVCN, -1).create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        return requestCaptor.getValue();
    }
}

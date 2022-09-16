package com.affirm.android;

import com.affirm.android.model.Checkout;
import com.google.common.truth.Truth;

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
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\"}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"%s\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\"}}";

    private static final String expectedCheckoutWithCaasBody =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\"}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"%s\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"caas\":\"4626b631-c5bc-4c4e-800b-dd5fa27ef8b8\",\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\"}}";

    private static final String expectedCheckoutWithCardAuthWindowBody =
            "{\"checkout\":{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\"}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\",\"platform_type\":\"Affirm Android SDK\",\"platform_affirm\":\"%s\"},\"merchant\":{\"public_api_key\":\"Y8CQXFF044903JC0\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"card_auth_window\":10,\"user_confirmation_url_action\":\"GET\"},\"api_version\":\"v2\"}}";

    private static String generateExpectedCheckoutBody() {
        return String.format(expectedCheckoutBody, BuildConfig.VERSION_NAME);
    }

    private static String generateExpectedCheckoutWithCaasBody() {
        return String.format(expectedCheckoutWithCaasBody, BuildConfig.VERSION_NAME);
    }

    private static String generateExpectedCheckoutWithCardAuthWindowBody() {
        return String.format(expectedCheckoutWithCardAuthWindowBody, BuildConfig.VERSION_NAME);
    }

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
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
}


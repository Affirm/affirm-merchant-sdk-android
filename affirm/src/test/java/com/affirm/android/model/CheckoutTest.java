package com.affirm.android.model;

import com.affirm.android.CheckoutFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CheckoutTest {

    private static final String expectCheckout = "{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\"}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testCheckoutToJson() {
        final Checkout checkout = CheckoutFactory.create();
        assertEquals(gson.toJson(checkout), expectCheckout);
    }
}

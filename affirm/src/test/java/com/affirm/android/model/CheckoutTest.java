package com.affirm.android.model;

import android.os.Parcel;

import com.affirm.android.CheckoutFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class CheckoutTest {

    private static final String expectCheckout = "{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testCheckoutToJson() {
        final Checkout checkout = CheckoutFactory.create();
        assertEquals(gson.toJson(checkout), expectCheckout);
    }

    @Test
    public void testCheckoutWithAccountLinkingIdToJson() {
        final JsonObject json = gson.toJsonTree(checkoutWithAccountLinkingId()).getAsJsonObject();
        assertEquals("link_123", json.get("account_linking_id").getAsString());
    }

    @Test
    public void testCheckoutWithoutAccountLinkingIdOmitsField() {
        final JsonObject json = gson.toJsonTree(CheckoutFactory.create()).getAsJsonObject();
        assertFalse(json.has("account_linking_id"));
    }

    @Test
    public void testAccountLinkingIdSurvivesParcel() {
        final Parcel parcel = Parcel.obtain();
        try {
            checkoutWithAccountLinkingId().writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            final Checkout restored = AutoValue_Checkout.CREATOR.createFromParcel(parcel);
            assertEquals("link_123", restored.accountLinkingId());
        } finally {
            parcel.recycle();
        }
    }

    private static Checkout checkoutWithAccountLinkingId() {
        return Checkout.builder()
                .setItems(new HashMap<>())
                .setShippingAmount(0)
                .setTaxAmount(0)
                .setTotal(BigDecimal.valueOf(10.0))
                .setSendShippingAddresses(false)
                .setAccountLinkingId("link_123")
                .build();
    }
}

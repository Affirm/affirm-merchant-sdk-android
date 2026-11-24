package com.affirm.android.model;

import android.os.Parcel;

import com.affirm.android.CheckoutFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class CheckoutTest {

    private static final String expectCheckout = "{\"items\":{\"wheel\":{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\",\"categories\":[[\"Apparel\",\"Pants\"],[\"Mens\",\"Apparel\",\"Pants\"]]}},\"currency\":\"USD\",\"shipping\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"billing\":{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}},\"shipping_amount\":100000,\"tax_amount\":10000,\"total\":110000,\"metadata\":{\"entity_name\":\"internal-sub_brand-name\",\"shipping_type\":\"UPS Ground\",\"webhook_session_id\":\"ABC123\"},\"checkoutMode\":\"STANDARD\"}";

    private static final String CALLBACK_URL = "https://merchant.com/shipping-and-totals";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testCheckoutToJson() {
        final Checkout checkout = CheckoutFactory.create();
        assertEquals(gson.toJson(checkout), expectCheckout);
    }

    @Test
    public void testBuilderDefaultsToStandardMode() {
        final Checkout checkout = CheckoutFactory.create();
        assertEquals(Checkout.CheckoutMode.STANDARD, checkout.checkoutMode());
        assertNull(checkout.shippingAndTotalsCallbackUrl());
    }

    @Test
    public void testCreateExpressCheckout() {
        final Checkout checkout = CheckoutFactory.createExpress();
        assertEquals(Checkout.CheckoutMode.EXPRESS, checkout.checkoutMode());
        assertEquals("order-123", checkout.orderId());
        assertEquals(CALLBACK_URL, checkout.shippingAndTotalsCallbackUrl());
        assertEquals("100000", checkout.metadata().get("subtotal"));
        assertNull(checkout.shippingAddress());
    }

    @Test
    public void testExpressCheckoutSurvivesParcel() {
        final Checkout checkout = CheckoutFactory.createExpress();
        final Parcel parcel = Parcel.obtain();
        checkout.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final Checkout restored = AutoValue_Checkout.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertEquals(checkout, restored);
    }

    @Test
    public void testExpressRequiresOrderId() {
        final IllegalStateException e = assertIllegalState(
                () -> expressBuilder().setOrderId(null).build());
        assertEquals("Express Checkout requires orderId", e.getMessage());

        assertIllegalState(() -> expressBuilder().setOrderId("").build());
    }

    @Test
    public void testExpressRequiresCallbackUrl() {
        final IllegalStateException e = assertIllegalState(
                () -> expressBuilder().setShippingAndTotalsCallbackUrl(null).build());
        assertEquals("Express Checkout requires shippingAndTotalsCallbackUrl", e.getMessage());

        assertIllegalState(() -> expressBuilder().setShippingAndTotalsCallbackUrl("").build());
    }

    @Test
    public void testExpressRequiresItems() {
        final IllegalStateException e = assertIllegalState(
                () -> expressBuilder().setItems(new HashMap<>()).build());
        assertEquals("Express Checkout requires items", e.getMessage());
    }

    @Test
    public void testExpressRequiresSubtotal() {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("entity_name", "internal-sub_brand-name");
        final IllegalStateException e = assertIllegalState(
                () -> expressBuilder().setMetadata(metadata).build());
        assertEquals("Express Checkout requires metadata[\"subtotal\"]", e.getMessage());

        assertIllegalState(
                () -> expressBuilder().setMetadata(null).build());

        metadata.put("subtotal", "");
        assertIllegalState(() -> expressBuilder().setMetadata(metadata).build());
    }

    @Test
    public void testExpressReportsAllMissingFields() {
        final IllegalStateException e = assertIllegalState(() -> expressBuilder()
                .setOrderId(null)
                .setShippingAndTotalsCallbackUrl(null)
                .setItems(new HashMap<>())
                .setMetadata(null)
                .build());
        assertEquals("Express Checkout requires orderId, shippingAndTotalsCallbackUrl, items, "
                + "metadata[\"subtotal\"]", e.getMessage());
    }

    @Test
    public void testExpressDoesNotRequireShipping() {
        final Checkout checkout = expressBuilder().build();
        assertNull(checkout.shippingAddress());
    }

    @Test
    public void testStandardStillRequiresShipping() {
        try {
            Checkout.builder()
                    .setItems(CheckoutFactory.expressItems())
                    .setTotal(BigDecimal.ZERO)
                    .setShippingAmount(BigDecimal.ZERO)
                    .setTaxAmount(BigDecimal.ZERO)
                    .build();
        } catch (NullPointerException e) {
            assertEquals("Null shipping", e.getMessage());
            return;
        }
        throw new AssertionError("Expected NullPointerException");
    }

    @Test
    public void testStandardCheckoutSkipsExpressValidation() {
        final Checkout checkout = Checkout.builder()
                .setItems(new HashMap<>())
                .setSendShippingAddresses(false)
                .setTotal(BigDecimal.ZERO)
                .setShippingAmount(BigDecimal.ZERO)
                .setTaxAmount(BigDecimal.ZERO)
                .build();
        assertEquals(Checkout.CheckoutMode.STANDARD, checkout.checkoutMode());
    }

    private static IllegalStateException assertIllegalState(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalStateException e) {
            return e;
        }
        throw new AssertionError("Expected IllegalStateException");
    }

    private static Checkout.Builder expressBuilder() {
        return Checkout.builder()
                .setCheckoutMode(Checkout.CheckoutMode.EXPRESS)
                .setItems(CheckoutFactory.expressItems())
                .setOrderId("order-123")
                .setShippingAndTotalsCallbackUrl(CALLBACK_URL)
                .setMetadata(CheckoutFactory.expressMetadata())
                .setTotal(BigDecimal.ZERO)
                .setShippingAmount(BigDecimal.ZERO)
                .setTaxAmount(BigDecimal.ZERO);
    }
}

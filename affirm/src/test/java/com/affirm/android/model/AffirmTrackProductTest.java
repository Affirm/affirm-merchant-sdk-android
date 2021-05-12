package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AffirmTrackProductTest {

    private static final String affirmTrackProductJson = "{\"productId\":\"SKU-1234\",\"brand\":\"Affirm\",\"category\":\"Apparel\",\"coupon\":\"SUMMER2018\",\"name\":\"Affirm T-Shirt\",\"price\":730,\"quantity\":1,\"variant\":\"Black\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testAffirmTrackProductToJson() {
        final AffirmTrackProduct affirmTrackProduct = AffirmTrackProduct.builder()
                .setBrand("Affirm")
                .setCategory("Apparel")
                .setCoupon("SUMMER2018")
                .setName("Affirm T-Shirt")
                .setPrice(730)
                .setProductId("SKU-1234")
                .setQuantity(1)
                .setVariant("Black")
                .build();
        assertEquals(gson.toJson(affirmTrackProduct), affirmTrackProductJson);
    }

    @Test
    public void testAffirmTrackProductParseFromJson() {
        AffirmTrackProduct affirmTrackOrder = gson.fromJson(affirmTrackProductJson, AffirmTrackProduct.class);
        System.out.println(affirmTrackOrder);
        Assert.assertNotNull(affirmTrackOrder);
        Assert.assertEquals(affirmTrackOrder.brand(), "Affirm");
        Assert.assertEquals(affirmTrackOrder.category(), "Apparel");
        Assert.assertEquals(affirmTrackOrder.coupon(), "SUMMER2018");
        Assert.assertEquals(affirmTrackOrder.name(), "Affirm T-Shirt");
        Assert.assertEquals(affirmTrackOrder.price(), (Integer) 730);
        Assert.assertEquals(affirmTrackOrder.productId(), "SKU-1234");
        Assert.assertEquals(affirmTrackOrder.quantity(), (Integer) 1);
        Assert.assertEquals(affirmTrackOrder.variant(), "Black");
    }
}

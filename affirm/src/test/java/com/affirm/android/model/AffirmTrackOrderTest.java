package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AffirmTrackOrderTest {

    private static final String affirmTrackOrderJson = "{\"storeName\":\"Affirm Store\",\"orderId\":\"T12345\",\"paymentMethod\":\"Visa\",\"coupon\":\"SUMMER2018\",\"currency\":\"USD\",\"discount\":0,\"revenue\":2920,\"shipping\":534,\"shippingMethod\":\"Fedex\",\"tax\":285,\"total\":3739}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testAffirmTrackOrderToJson() {
        final AffirmTrackOrder affirmTrackOrder = AffirmTrackOrder.builder()
                .setStoreName("Affirm Store")
                .setCoupon("SUMMER2018")
                .setCurrency(Currency.USD)
                .setDiscount(0)
                .setPaymentMethod("Visa")
                .setRevenue(2920)
                .setShipping(534)
                .setShippingMethod("Fedex")
                .setTax(285)
                .setOrderId("T12345")
                .setTotal(3739)
                .build();
        assertEquals(gson.toJson(affirmTrackOrder), affirmTrackOrderJson);
    }

    @Test
    public void testAffirmTrackOrderParseFromJson() {
        AffirmTrackOrder affirmTrackOrder = gson.fromJson(affirmTrackOrderJson, AffirmTrackOrder.class);
        System.out.println(affirmTrackOrder);
        Assert.assertNotNull(affirmTrackOrder);
        Assert.assertEquals(affirmTrackOrder.storeName(), "Affirm Store");
        Assert.assertEquals(affirmTrackOrder.coupon(), "SUMMER2018");
        Assert.assertEquals(affirmTrackOrder.currency(), Currency.USD);
        Assert.assertEquals(affirmTrackOrder.discount(), (Integer) 0);
        Assert.assertEquals(affirmTrackOrder.paymentMethod(), "Visa");
        Assert.assertEquals(affirmTrackOrder.revenue(), (Integer) 2920);
        Assert.assertEquals(affirmTrackOrder.shipping(), (Integer) 534);
        Assert.assertEquals(affirmTrackOrder.shippingMethod(), "Fedex");
        Assert.assertEquals(affirmTrackOrder.tax(), (Integer) 285);
        Assert.assertEquals(affirmTrackOrder.orderId(), "T12345");
        Assert.assertEquals(affirmTrackOrder.total(), (Integer) 3739);
    }
}

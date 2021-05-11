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
public class PromoConfigTest {

    private static final String promoConfigJson = "{\"promo_prequal_enabled\":true,\"promo_style\":\"abc\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testItemToJson() {
        final PromoConfig promoConfig = PromoConfig.builder()
                .setPromoStyle("abc")
                .setPromoPrequalEnabled(true)
                .build();
        assertEquals(gson.toJson(promoConfig), promoConfigJson);
    }

    @Test
    public void testItemParseFromJson() {
        PromoConfig promoConfig = gson.fromJson(promoConfigJson, PromoConfig.class);
        System.out.println(promoConfig);
        Assert.assertNotNull(promoConfig);
        Assert.assertEquals(promoConfig.promoStyle(), "abc");
        Assert.assertTrue(promoConfig.promoPrequalEnabled());
    }
}

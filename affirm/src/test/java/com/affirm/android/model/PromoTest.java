package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PromoTest {

    private static final String promoJson = "{\"ala\":\"def\",\"html_ala\":\"abc\",\"config\":{\"promo_prequal_enabled\":true,\"promo_style\":\"abc\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testItemToJson() {
        final PromoConfig promoConfig = PromoConfig.builder()
                .setPromoStyle("abc")
                .setPromoPrequalEnabled(true)
                .build();
        final Promo promo = Promo.builder()
                .setPromoConfig(promoConfig)
                .setHtmlAla("abc")
                .setAla("def")
                .build();
        assertEquals(gson.toJson(promo), promoJson);
    }

    @Test
    public void testItemParseFromJson() {
        Promo promo = gson.fromJson(promoJson, Promo.class);
        System.out.println(promo);
        Assert.assertNotNull(promo);

        PromoConfig promoConfig = promo.promoConfig();
        Assert.assertEquals(promoConfig.promoStyle(), "abc");
        Assert.assertTrue(promoConfig.promoPrequalEnabled());

        Assert.assertEquals(promo.htmlAla(), "abc");
        Assert.assertEquals(promo.ala(), "def");
    }
}

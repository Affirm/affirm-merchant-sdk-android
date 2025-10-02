package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PromoConfigTest {

    private static final String promoConfigJson = "{\"promo_prequal_enabled\":true,\"promo_style\":\"abc\"}";
    private static final String promoConfigJsonNoStyle = "{\"promo_prequal_enabled\":true}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testItemToJson() {
        final PromoConfig promoConfig = PromoConfig.builder()
                .setPromoStyle("abc")
                .setPromoPrequalEnabled(true)
                .build();
        assertEquals(promoConfigJson, gson.toJson(promoConfig));
    }

    @Test
    public void testItemParseFromJson() {
        PromoConfig promoConfig = gson.fromJson(promoConfigJson, PromoConfig.class);
        Assert.assertNotNull(promoConfig);
        Assert.assertEquals("abc", promoConfig.promoStyle());
        Assert.assertTrue(promoConfig.promoPrequalEnabled());
    }

    @Test
    public void testItemToJsonWithNullPromoStyle() {
        final PromoConfig promoConfig = PromoConfig.builder()
                .setPromoStyle(null)
                .setPromoPrequalEnabled(true)
                .build();
        assertEquals(promoConfigJsonNoStyle, gson.toJson(promoConfig));
    }

    @Test
    public void testItemParseFromJsonWithoutPromoStyle() {
        PromoConfig promoConfig = gson.fromJson(promoConfigJsonNoStyle, PromoConfig.class);
        Assert.assertNotNull(promoConfig);
        Assert.assertNull(promoConfig.promoStyle());
        Assert.assertTrue(promoConfig.promoPrequalEnabled());
    }
}

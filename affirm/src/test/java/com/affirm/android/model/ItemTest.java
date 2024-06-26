package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ItemTest {

    private static final String itemJson = "{\"display_name\":\"Great Deal Wheel\",\"sku\":\"wheel\",\"unit_price\":100000,\"qty\":1,\"item_url\":\"http://merchant.com/great_deal_wheel\",\"item_image_url\":\"http://www.image.com/111\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testItemToJson() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("http://www.image.com/111")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(BigDecimal.valueOf(1000.0))
                .setUrl("http://merchant.com/great_deal_wheel")
                .build();
        assertEquals(gson.toJson(item), itemJson);
    }
}

package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CheckoutResponseTest {

    private static final String checkoutResponseJson = "{\"redirect_url\":\"http://www.abc.com/123\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testItemToJson() {
        final CheckoutResponse checkoutResponse = CheckoutResponse.builder()
                .setRedirectUrl("http://www.abc.com/123")
                .build();
        assertEquals(gson.toJson(checkoutResponse), checkoutResponseJson);
    }

    @Test
    public void testItemParseFromJson() {
        CheckoutResponse checkoutResponse = gson.fromJson(checkoutResponseJson, CheckoutResponse.class);
        System.out.println(checkoutResponse);
        Assert.assertNotNull(checkoutResponse);
        Assert.assertEquals(checkoutResponse.redirectUrl(), "http://www.abc.com/123");
    }
}

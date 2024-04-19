package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VcnReasonTest {

    private static final String vcnReasonJson = "{\"reason\":\"def\",\"checkout_token\":\"abc\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testItemToJson() {
        final VcnReason vcnReason = VcnReason.builder()
                .setCheckoutToken("abc")
                .setReason("def")
                .build();
        assertEquals(gson.toJson(vcnReason), vcnReasonJson);
    }

    @Test
    public void testItemParseFromJson() {
        VcnReason vcnReason = gson.fromJson(vcnReasonJson, VcnReason.class);
        System.out.println(vcnReason);
        Assert.assertNotNull(vcnReason);
        Assert.assertEquals(vcnReason.checkoutToken(), "abc");
        Assert.assertEquals(vcnReason.reason(), "def");
    }
}

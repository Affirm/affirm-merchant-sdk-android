package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class MerchantTest {

    private static final String vcnMerchantitemJson = "{\"public_api_key\":\"abc\",\"name\":\"def\",\"use_vcn\":true,\"caas\":\"caas\",\"card_auth_window\":1}";
    private static final String nonVcnMerchantitemJson = "{\"public_api_key\":\"abc\",\"user_confirmation_url\":\"affirm://checkout/confirmed\",\"user_cancel_url\":\"affirm://checkout/cancelled\",\"name\":\"def\",\"caas\":\"caas\",\"card_auth_window\":1}";
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testVcnMerchantToJson() {
        final Merchant merchant = Merchant.builder()
                .setPublicApiKey("abc")
                .setUseVcn(true)
                .setName("def")
                .setCaas("caas")
                .setCardAuthWindow(1)
                .build();
        assertEquals(gson.toJson(merchant), vcnMerchantitemJson);
    }

    @Test
    public void testNonVcnMerchantToJson() {
        final Merchant merchant = Merchant.builder()
                .setPublicApiKey("abc")
                .setConfirmationUrl("affirm://checkout/confirmed")
                .setCancelUrl("affirm://checkout/cancelled")
                .setName("def")
                .setCaas("caas")
                .setCardAuthWindow(1)
                .build();
        assertEquals(gson.toJson(merchant), nonVcnMerchantitemJson);
    }

    @Test
    public void testVcnMerchantParseFromJson() {
        Merchant merchant = gson.fromJson(vcnMerchantitemJson, Merchant.class);
        System.out.println(merchant);
        Assert.assertNotNull(merchant);
        Assert.assertEquals(merchant.publicApiKey(), "abc");
        Assert.assertEquals(merchant.useVcn(), true);
        Assert.assertEquals(merchant.name(), "def");
        Assert.assertEquals(merchant.caas(), "caas");
        Assert.assertEquals(merchant.cardAuthWindow(), (Integer) 1);
    }

    @Test
    public void testNonVcnMerchantParseFromJson() {
        Merchant merchant = gson.fromJson(nonVcnMerchantitemJson, Merchant.class);
        System.out.println(merchant);
        Assert.assertNotNull(merchant);
        Assert.assertEquals(merchant.publicApiKey(), "abc");
        Assert.assertNull(merchant.useVcn());
        Assert.assertEquals(merchant.confirmationUrl(), "affirm://checkout/confirmed");
        Assert.assertEquals(merchant.cancelUrl(), "affirm://checkout/cancelled");
        Assert.assertEquals(merchant.name(), "def");
        Assert.assertEquals(merchant.caas(), "caas");
        Assert.assertEquals(merchant.cardAuthWindow(), (Integer) 1);
    }
}
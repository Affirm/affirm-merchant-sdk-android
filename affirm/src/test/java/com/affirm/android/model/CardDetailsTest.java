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
public class CardDetailsTest {

    private static final String cardDetailsJson = "{\"cardholder_name\":\"AffirmInc Hector Montserrate\",\"checkout_token\":\"YP99FF9TAMU2Q4CJ\",\"cvv\":\"123\",\"expiration\":\"0719\",\"number\":\"4012888888881881\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testItemToJson() {
        final CardDetails cardDetails = CardDetails.builder()
                .setCardholderName("AffirmInc Hector Montserrate")
                .setCheckoutToken("YP99FF9TAMU2Q4CJ")
                .setCvv("123")
                .setNumber("4012888888881881")
                .setExpiration("0719")
                .build();
        assertEquals(gson.toJson(cardDetails), cardDetailsJson);
    }

    @Test
    public void testItemParseFromJson() {
        CardDetails cardDetails = gson.fromJson(cardDetailsJson, CardDetails.class);
        System.out.println(cardDetails);
        Assert.assertNotNull(cardDetails);
        Assert.assertEquals(cardDetails.cardholderName(), "AffirmInc Hector Montserrate");
        Assert.assertEquals(cardDetails.checkoutToken(), "YP99FF9TAMU2Q4CJ");
        Assert.assertEquals(cardDetails.cvv(), "123");
        Assert.assertEquals(cardDetails.number(), "4012888888881881");
        Assert.assertEquals(cardDetails.expiration(), "0719");
    }
}

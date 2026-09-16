package com.affirm.android;

import android.content.Intent;

import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.AffirmError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CheckoutErrorIntentTest {

    private static final String affirmErrorWithUiJson = "{\"status_code\":400,\"type\":\"invalid_request\",\"code\":\"invalid_field\",\"field\":\"shipping.address\",\"message\":\"Your shipping address is invalid.\",\"ui\":{\"sub\":\"Please return to Your Customer-Facing Merchant Name to correct the shipping address you entered:\",\"main\":\"We could not validate your shipping address\",\"sub_extra\":[\"123 Fake St\",\"San Chicago, WA 11223\"]}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testCheckoutErrorIntentRoundTrip() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);

        Intent intent = new Intent();
        intent.putExtra(AffirmConstants.CHECKOUT_ERROR_DETAIL, affirmError);

        AffirmError recreated = intent.getParcelableExtra(AffirmConstants.CHECKOUT_ERROR_DETAIL);

        Assert.assertNotNull(recreated);
        Assert.assertEquals(affirmError.message(), recreated.message());
        Assert.assertEquals(affirmError.status(), recreated.status());
        Assert.assertEquals(affirmError.field(), recreated.field());
        Assert.assertNotNull(recreated.ui());
        Assert.assertEquals(affirmError.ui().main(), recreated.ui().main());
        Assert.assertEquals(affirmError.ui().sub(), recreated.ui().sub());
        Assert.assertEquals(affirmError.ui().subExtra(), recreated.ui().subExtra());
    }
}

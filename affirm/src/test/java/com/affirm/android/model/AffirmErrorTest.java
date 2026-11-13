package com.affirm.android.model;

import android.os.Parcel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AffirmErrorTest {

    private static final String affirmErrorJson = "{\"status_code\":401,\"type\":\"unauthorized\",\"code\":\"auth-declined\",\"message\":\"message\"}";
    private static final String affirmErrorWithUiJson = "{\"status_code\":400,\"type\":\"invalid_request\",\"code\":\"invalid_field\",\"field\":\"shipping.address\",\"message\":\"Your shipping address is invalid.\",\"ui\":{\"sub\":\"Please return to Your Customer-Facing Merchant Name to correct the shipping address you entered:\",\"main\":\"We could not validate your shipping address\",\"sub_extra\":[\"123 Fake St\",\"San Chicago, WA 11223\"]}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testItemParseFromJson() {
        AffirmError affirmError = gson.fromJson(affirmErrorJson, AffirmError.class);
        System.out.println(affirmError);
        Assert.assertNotNull(affirmError);
        Assert.assertEquals(affirmError.status(), (Integer)401);
        Assert.assertEquals(affirmError.type(), "unauthorized");
        Assert.assertEquals(affirmError.code(), "auth-declined");
        Assert.assertEquals(affirmError.message(), "message");
    }

    @Test
    public void testParseFromJsonWithUi() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);
        Assert.assertNotNull(affirmError);
        Assert.assertNotNull(affirmError.ui());
        Assert.assertEquals("We could not validate your shipping address", affirmError.ui().main());
        Assert.assertEquals("Please return to Your Customer-Facing Merchant Name to correct the shipping address you entered:",
                affirmError.ui().sub());
        Assert.assertEquals("123 Fake St", affirmError.ui().subExtra().get(0));
        Assert.assertEquals("San Chicago, WA 11223", affirmError.ui().subExtra().get(1));
    }

    @Test
    public void testToStringExcludesUiToKeepCustomerAddressOutOfLogsAndTracking() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);

        String description = affirmError.toString();

        Assert.assertTrue(description.contains("Your shipping address is invalid."));
        Assert.assertFalse(description.contains("123 Fake St"));
        Assert.assertFalse(description.contains("We could not validate your shipping address"));
    }

    @Test
    public void testParcelableRoundTripWithUi() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);
        Parcel parcel = Parcel.obtain();
        parcel.writeParcelable(affirmError, 0);
        parcel.setDataPosition(0);

        AffirmError recreated = parcel.readParcelable(AffirmError.class.getClassLoader());
        parcel.recycle();

        Assert.assertNotNull(recreated);
        Assert.assertEquals(affirmError.message(), recreated.message());
        Assert.assertEquals(affirmError.status(), recreated.status());
        Assert.assertNotNull(recreated.ui());
        Assert.assertEquals(affirmError.ui().main(), recreated.ui().main());
        Assert.assertEquals(affirmError.ui().sub(), recreated.ui().sub());
        Assert.assertEquals(affirmError.ui().subExtra(), recreated.ui().subExtra());
    }
}

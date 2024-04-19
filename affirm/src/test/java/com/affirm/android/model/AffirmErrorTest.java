package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AffirmErrorTest {

    private static final String affirmErrorJson = "{\"status_code\":401,\"type\":\"unauthorized\",\"code\":\"auth-declined\",\"message\":\"message\"}";

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
}

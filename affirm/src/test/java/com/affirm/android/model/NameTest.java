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
public class NameTest {

    private static final String nameJson = "{\"full\":\"John Smith\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testItemToJson() {
        final Name name = Name.builder().setFull("John Smith").build();
        assertEquals(gson.toJson(name), nameJson);
    }

    @Test
    public void testItemParseFromJson() {
        Name name = gson.fromJson(nameJson, Name.class);
        System.out.println(name);
        Assert.assertNotNull(name);
        Assert.assertEquals(name.full(), "John Smith");
    }
}

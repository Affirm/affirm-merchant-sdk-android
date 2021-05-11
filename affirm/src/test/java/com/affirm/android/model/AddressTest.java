package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AddressTest {

    private static final String addressJson = "{\"line1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"state\":\"CA\",\"zipcode\":\"94103\",\"country\":\"USA\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testAddressToJson() {
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94103")
                .build();
        assertEquals(gson.toJson(address), addressJson);
    }

    @Test
    public void testAddressParseFromJson() {
        Address address = gson.fromJson(addressJson, Address.class);
        System.out.println(address);
        Assert.assertNotNull(address);
        Assert.assertEquals(address.line1(), "333 Kansas st");
        Assert.assertNull(address.line2());
        Assert.assertEquals(address.city(), "San Francisco");
        Assert.assertEquals(address.state(), "CA");
        Assert.assertEquals(address.zipcode(), "94103");
        Assert.assertEquals(address.country(), "USA");
    }
}
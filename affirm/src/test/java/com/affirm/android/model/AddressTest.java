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

    private static final String addressJson = "{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testAddressToJson() {
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setStreet1("333 Kansas st")
                .setRegion1Code("CA")
                .setPostalCode("94103")
                .build();
        assertEquals(gson.toJson(address), addressJson);
    }

    @Test
    public void testAddressParseFromJson() {
        Address address = gson.fromJson(addressJson, Address.class);
        System.out.println(address);
        Assert.assertNotNull(address);
        Assert.assertEquals(address.street1(), "333 Kansas st");
        Assert.assertNull(address.street2());
        Assert.assertEquals(address.city(), "San Francisco");
        Assert.assertEquals(address.region1Code(), "CA");
        Assert.assertEquals(address.postalCode(), "94103");
        Assert.assertEquals(address.country(), "USA");
    }
}
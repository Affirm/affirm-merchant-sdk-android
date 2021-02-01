package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class CAAddressTest {

    private static final String caAddressJson = "{\"street1\":\"123 Alder Creek Dr.\",\"street2\":\"Floor 7\",\"city\":\"Toronto\",\"region1_code\":\"ON\",\"postal_code\":\"M4B 1B3\",\"country_code\":\"CA\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testAddressToJson() {
        final AbstractAddress address = CAAddress.builder()
                .setStreet1("123 Alder Creek Dr.")
                .setStreet2("Floor 7")
                .setCity("Toronto")
                .setRegion1Code("ON")
                .setPostalCode("M4B 1B3")
                .setCountryCode("CA")
                .build();
        assertEquals(gson.toJson(address), caAddressJson);
    }

    @Test
    public void testAddressParseFromJson() {
        CAAddress address = gson.fromJson(caAddressJson, CAAddress.class);
        System.out.println(address);
        Assert.assertNotNull(address);
        Assert.assertEquals(address.street1(), "123 Alder Creek Dr.");
        Assert.assertEquals(address.street2(), "Floor 7");
        Assert.assertEquals(address.city(), "Toronto");
        Assert.assertEquals(address.region1Code(), "ON");
        Assert.assertEquals(address.postalCode(), "M4B 1B3");
        Assert.assertEquals(address.countryCode(), "CA");
    }
}
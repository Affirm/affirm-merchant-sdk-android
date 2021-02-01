package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class BillingTest {

    private static final String billingJson = "{\"address\":{\"line1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"state\":\"CA\",\"zipcode\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
            .create();

    @Test
    public void testBillingToJson() {
        final Name name = Name.builder().setFull("John Smith").build();
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setLine1("333 Kansas st")
                .setState("CA")
                .setZipcode("94103")
                .build();

        final Billing billing = Billing.builder().setAddress(address).setName(name).build();
        assertEquals(gson.toJson(billing), billingJson);
    }
}

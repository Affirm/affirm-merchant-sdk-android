package com.affirm.android.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class BillingTest {

    private static final String billingJson = "{\"address\":{\"street1\":\"333 Kansas st\",\"city\":\"San Francisco\",\"region1_code\":\"CA\",\"postal_code\":\"94103\",\"country\":\"USA\"},\"name\":{\"full\":\"John Smith\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testBillingToJson() {
        final Name name = Name.builder().setFull("John Smith").build();
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry("USA")
                .setStreet1("333 Kansas st")
                .setRegion1Code("CA")
                .setPostalCode("94103")
                .build();

        final Billing billing = Billing.builder().setAddress(address).setName(name).build();
        assertEquals(gson.toJson(billing), billingJson);
    }
}

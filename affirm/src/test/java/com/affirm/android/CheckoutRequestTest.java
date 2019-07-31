package com.affirm.android;

import com.affirm.android.utils.CheckoutFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CheckoutRequestTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testCheckout() {
        CheckoutRequest checkoutRequest = new CheckoutRequest(CheckoutFactory.create(), null, false);
        checkoutRequest.create();
    }
}


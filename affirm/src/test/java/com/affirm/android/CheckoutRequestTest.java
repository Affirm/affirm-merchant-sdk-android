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
        CheckoutRequest checkoutRequest = new CheckoutRequest(CheckoutFactory.create(), null, null, false, null);
        checkoutRequest.create();
    }

    @Test
    public void testCheckoutWithCaas() {
        CheckoutRequest checkoutRequest = new CheckoutRequest(CheckoutFactory.create(), null, "4626b631-c5bc-4c4e-800b-dd5fa27ef8b8", false, null);
        checkoutRequest.create();
    }

    @Test
    public void testCheckoutWithCardAuthWindow() {
        CheckoutRequest checkoutRequest = new CheckoutRequest(CheckoutFactory.create(), null, null, false, 10);
        checkoutRequest.create();
    }
}


package com.affirm.android.model;

import com.affirm.android.CheckoutFactory;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// No Robolectric: merchants build checkouts in plain JVM unit tests, where Android APIs throw.
public class ExpressCheckoutJvmTest {

    @Test
    public void testCreateExpressCheckout() {
        final Checkout checkout = CheckoutFactory.createExpress();
        assertEquals(Checkout.CheckoutMode.EXPRESS, checkout.checkoutMode());
    }

    @Test
    public void testMissingFields() {
        try {
            Checkout.createExpressCheckout(new HashMap<>(), "", "", new HashMap<>());
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Express Checkout requires orderId, shippingAndTotalsCallbackUrl, items, "
                    + "metadata[\"subtotal\"]", e.getMessage());
        }
    }
}

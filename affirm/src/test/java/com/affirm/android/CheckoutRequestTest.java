package com.affirm.android;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.utils.CheckoutFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class CheckoutRequestTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder()
                    .setPublicKey("Y8CQXFF044903JC0")
                    .build()
            );
        }
    }

    @Test
    public void testGetNewPromo() throws ConnectionException, APIException, InvalidRequestException, PermissionException {
        CheckoutResponse response = AffirmApiHandler.executeCheckout(CheckoutFactory.create());
        assertNotNull(response);
    }
}


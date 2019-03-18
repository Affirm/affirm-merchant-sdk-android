package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AffirmTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder()
                    .setPublicKey("sdf")
                    .build()
            );
        }
    }

    @Test
    public void onActivityResult_Success() {
        Affirm.CheckoutCallbacks callbacks = Mockito.mock(Affirm.CheckoutCallbacks.class);

        Intent intent = Mockito.mock(Intent.class);

        Mockito.when(intent.getStringExtra(Mockito.any(String.class))).thenReturn("1234");

        Affirm.handleCheckoutData(callbacks, 8076, Activity.RESULT_OK, intent);

        Mockito.verify(callbacks).onAffirmCheckoutSuccess("1234");
    }

    @Test
    public void onActivityResult_Cancelled() {
        Affirm.CheckoutCallbacks callbacks = Mockito.mock(Affirm.CheckoutCallbacks.class);

        Affirm.handleCheckoutData(callbacks, 8076, Activity.RESULT_CANCELED, Mockito.mock(Intent.class));

        Mockito.verify(callbacks).onAffirmCheckoutCancelled();
    }

    @Test
    public void onActivityResult_Error() {
        Affirm.CheckoutCallbacks callbacks = Mockito.mock(Affirm.CheckoutCallbacks.class);

        Intent intent = Mockito.mock(Intent.class);

        Mockito.when(intent.getStringExtra(Mockito.any(String.class))).thenReturn("error");

        Affirm.handleCheckoutData(callbacks, 8076, CheckoutActivity.RESULT_ERROR, intent);

        Mockito.verify(callbacks).onAffirmCheckoutError("error");
    }
}
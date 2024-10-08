package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

public class AffirmTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testSetPublicKey() {
        Affirm.setPublicKey("Y8CQXFF044903JC1");
        assertEquals("Y8CQXFF044903JC1", AffirmPlugins.get().publicKey());
    }

    @Test
    public void testSetPublicKeyAndMerchantName() {
        Affirm.setPublicKeyAndMerchantName("Y8CQXFF044903JC1", "aaa");
        assertEquals("Y8CQXFF044903JC1", AffirmPlugins.get().publicKey());
        assertEquals("aaa", AffirmPlugins.get().merchantName());
    }

    @Test
    public void testSetMerchantName() {
        Affirm.setMerchantName("aaa");
        assertEquals("aaa", AffirmPlugins.get().merchantName());
    }

    @Test
    public void testSetCountryCode() {
        Affirm.setCountryCode(Locale.US.getISO3Country());
        assertEquals(Locale.US.getISO3Country(), AffirmPlugins.get().countryCode());
    }

    @Test
    public void testSetLocale() {
        Affirm.setLocale(Locale.US.toString());
        assertEquals(Locale.US.toString(), AffirmPlugins.get().locale());
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

        Affirm.handleCheckoutData(callbacks, 8076, Affirm.RESULT_ERROR, intent);

        Mockito.verify(callbacks).onAffirmCheckoutError("error");
    }
}
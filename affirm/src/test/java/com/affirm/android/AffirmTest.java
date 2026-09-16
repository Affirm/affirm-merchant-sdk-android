package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.AffirmError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

public class AffirmTest {

    private static final String affirmErrorWithUiJson = "{\"status_code\":400,\"type\":\"invalid_request\",\"code\":\"invalid_field\",\"field\":\"shipping.address\",\"message\":\"Your shipping address is invalid.\",\"ui\":{\"sub\":\"Please return to Your Customer-Facing Merchant Name to correct the shipping address you entered:\",\"main\":\"We could not validate your shipping address\",\"sub_extra\":[\"123 Fake St\",\"San Chicago, WA 11223\"]}}";
    private static final String affirmErrorWithoutMessageJson = "{\"status_code\":400,\"type\":\"invalid_request\",\"ui\":{\"main\":\"We could not validate your shipping address\"}}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

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

        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);

        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getStringExtra(AffirmConstants.CHECKOUT_ERROR)).thenReturn("error");
        Mockito.when(intent.getParcelableExtra(AffirmConstants.CHECKOUT_ERROR_DETAIL))
                .thenReturn(affirmError);

        Affirm.handleCheckoutData(callbacks, 8076, Affirm.RESULT_ERROR, intent);

        Mockito.verify(callbacks).onAffirmCheckoutError("error", affirmError);
    }

    @Test
    public void onCheckoutError_DefaultCallbackDelegatesToLegacyCallback() {
        final String[] receivedMessage = new String[1];
        Affirm.CheckoutCallbacks callbacks = new Affirm.CheckoutCallbacks() {
            @Override
            public void onAffirmCheckoutError(String message) {
                receivedMessage[0] = message;
            }

            @Override
            public void onAffirmCheckoutCancelled() {
            }

            @Override
            public void onAffirmCheckoutSuccess(String token) {
            }
        };

        callbacks.onAffirmCheckoutError("error", null);

        assertEquals("error", receivedMessage[0]);
    }

    @Test
    public void checkoutErrorMessage_FallsBackToToStringForNullMessage() {
        ConnectionException exception = new ConnectionException(null);

        assertEquals(exception.toString(), CheckoutErrors.messageFrom(exception));
    }

    @Test
    public void checkoutErrorMessage_PrefersUiMainOverToStringForNullMessage() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithoutMessageJson, AffirmError.class);
        InvalidRequestException exception = invalidRequestException(affirmError);

        assertEquals("We could not validate your shipping address",
                CheckoutErrors.messageFrom(exception));
    }

    @Test
    public void checkoutError_UsesStructuredDetailsFromWrappedCause() {
        AffirmError affirmError = gson.fromJson(affirmErrorWithUiJson, AffirmError.class);
        InvalidRequestException cause = invalidRequestException(affirmError);
        APIException wrapper = new APIException(cause.getMessage(), cause);

        assertEquals("Your shipping address is invalid.", CheckoutErrors.messageFrom(wrapper));

        AffirmError recovered = CheckoutErrors.affirmErrorFrom(wrapper);
        assertEquals((Integer) 400, recovered.status());
        assertEquals("We could not validate your shipping address", recovered.ui().main());
        assertEquals("123 Fake St", recovered.ui().subExtra().get(0));
    }

    @Test
    public void checkoutError_IsNullWhenExceptionCarriesNoStructuredDetails() {
        assertEquals(null, CheckoutErrors.affirmErrorFrom(new ConnectionException("boom")));
    }

    private InvalidRequestException invalidRequestException(AffirmError affirmError) {
        return new InvalidRequestException(
                affirmError.message(),
                affirmError.type(),
                affirmError.fields(),
                affirmError.field(),
                "req_123",
                affirmError.status(),
                affirmError,
                null
        );
    }
}

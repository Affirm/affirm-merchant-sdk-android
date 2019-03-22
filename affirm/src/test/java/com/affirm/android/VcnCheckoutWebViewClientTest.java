package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VcnCheckoutWebViewClientTest {

    @Mock
    VcnCheckoutWebViewClient.Callbacks callbacks;
    @Mock
    WebView webview;

    private VcnCheckoutWebViewClient affirmWebViewClient;

    @Before
    public void setup() {
        Affirm.Configuration configuration = new Affirm.Configuration.Builder("111")
                .setEnvironment(Affirm.Environment.PRODUCTION)
                .build();
        AffirmPlugins plugins = new AffirmPlugins(configuration);
        Gson gson = plugins.gson();
        affirmWebViewClient = new VcnCheckoutWebViewClient(gson, callbacks);
    }

    @Test
    public void shouldOverrideUrlLoading_Confirmation() {
        final String encodedData =
                "%7B%22billing_address%22%3A%7B%22city%22%3A%22San%20Francisco%22%2C%22state%22%3A%22CA%22%2C%22zipcode%22%3A%2294104%22%2C%22line1%22%3A%22225%20Bush%20St%22%2C%22line2%22%3A%22Suite%201600%22%7D%2C%22checkout_token%22%3A%22YP99FF9TAMU2Q4CJ%22%2C%22created%22%3A%222017-07-12T15%3A55%3A49.809271Z%22%2C%22cvv%22%3A%22123%22%2C%22number%22%3A%224012888888881881%22%2C%22callback_id%22%3A%226054fe9a-53aa-48eb-a3fe-ad6290848431%22%2C%22cardholder_name%22%3A%22AffirmInc%20Hector%20Montserrate%22%2C%22expiration%22%3A%220719%22%2C%22id%22%3A%22YP99FF9TAMU2Q4CJ%22%7D";

        affirmWebViewClient.shouldOverrideUrlLoading(webview,
                "affirm://checkout/confirmed?data=" + encodedData);

        final CardDetails expected = CardDetails.builder()
                .setCardholderName("AffirmInc Hector Montserrate")
                .setCheckoutToken("YP99FF9TAMU2Q4CJ")
                .setCvv("123")
                .setNumber("4012888888881881")
                .setExpiration("0719")
                .build();
        Mockito.verify(callbacks).onWebViewConfirmation(expected);
    }

    @Test
    public void shouldOverrideUrlLoading_Cancellation() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview, "affirm://checkout/cancelled");
        Mockito.verify(callbacks).onWebViewCancellation();
    }

    @Test
    public void shouldOverrideUrlLoading_Random() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview, "http://www.affirm.com/api/v1/get");
        Mockito.verify(callbacks, never()).onWebViewConfirmation(any(CardDetails.class));
        Mockito.verify(callbacks, never()).onWebViewCancellation();
    }

    @Test
    public void onReceivedError() {
        WebResourceRequest resourceRequest = Mockito.mock(WebResourceRequest.class);
        WebResourceError error = Mockito.mock(WebResourceError.class);
        when(error.toString()).thenReturn("error msg");

        affirmWebViewClient.onReceivedError(webview, resourceRequest, error);
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }
}

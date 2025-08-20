package com.affirm.android;

import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardBillingAddress;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.VcnReason;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Uri.class)
public class VcnCheckoutWebViewClientTest {

    @Mock
    VcnCheckoutWebViewClient.Callbacks callbacks;
    @Mock
    WebView webview;

    private VcnCheckoutWebViewClient affirmWebViewClient;

    @Before
    public void setup() {
        mockUriParse();
        Affirm.Configuration configuration = new Affirm.Configuration.Builder("111", Affirm.Environment.SANDBOX)
                .build();
        AffirmPlugins plugins = new AffirmPlugins(configuration);
        Gson gson = plugins.gson();
        affirmWebViewClient = new VcnCheckoutWebViewClient(gson, "true", callbacks);
    }

    private void mockUriParse() {
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.when(Uri.parse(anyString())).then((Answer<Uri>) invocation -> mockUri((String) invocation.getArguments()[0]));
    }

    private Uri mockUri(final String url) {
        Uri mockUri = mock(Uri.class);
        when(mockUri.toString()).thenReturn(url);
        when(mockUri.getQueryParameter("data")).then((Answer<String>) invocation -> url.substring(url.indexOf("data=") + "data=".length()));
        return mockUri;
    }

    @Test
    public void shouldOverrideUrlLoading_Confirmation() {
        final String encodedData =
                "%7B%22checkout_token%22%3A%22S4FIKFJHE0HQBGRL%22%2C%22cvv%22%3A%22123%22%2C%22number%22%3A%224111111111111111%22%2C%22cardholder_name%22%3A%22AffirmInc%20sudhir%20yyyyy%22%2C%22expiration%22%3A%220921%22%2C%22callback_id%22%3A%22031efdda-b6ed-4593-8da4-8889306d60bb%22%2C%22id%22%3A%22S4FIKFJHE0HQBGRL%22%7D";

        affirmWebViewClient.shouldOverrideUrlLoading(webview,
                "affirm://checkout/confirmed?data=" + encodedData);

        final CardDetails expected = CardDetails.builder()
                .setCheckoutToken("S4FIKFJHE0HQBGRL")
                .setCvv("123")
                .setNumber("4111111111111111")
                .setCardholderName("AffirmInc sudhir yyyyy")
                .setExpiration("0921")
                .setCallbackId("031efdda-b6ed-4593-8da4-8889306d60bb")
                .setId("S4FIKFJHE0HQBGRL")
                .build();
        Mockito.verify(callbacks).onWebViewConfirmation(expected);
    }

    @Test
    public void shouldOverrideUrlLoading_ConfirmationWithBilling() {
        final String encodedData =
                "%7B%22checkout_token%22%3A%22TVY2I3JVYCW0FWYF%22%2C%22cvv%22%3A%22123%22%2C%22number%22%3A%224111111111111111%22%2C%22cardholder_name%22%3A%22AffirmInc%20John%20R%20Andrew%22%2C%22expiration%22%3A%220423%22%2C%22callback_id%22%3A%22cd5d5440-3d63-4e4c-9aa2-fbf16ea689e6%22%2C%22id%22%3A%22TVY2I3JVYCW0FWYF%22%2C%22billing_address%22%3A%7B%22line1%22%3A%22650%20California%20St.%22%2C%22line2%22%3A%2212th%20Floor%22%2C%22city%22%3A%22San%20Francisco%22%2C%22state%22%3A%22CA%22%2C%22zipcode%22%3A%2294108%22%7D%7D";

        affirmWebViewClient.shouldOverrideUrlLoading(webview,
                "affirm://checkout/confirmed?data=" + encodedData);

        final CardBillingAddress billingAddress = CardBillingAddress.builder()
                .setLine1("650 California St.")
                .setLine2("12th Floor")
                .setCity("San Francisco")
                .setState("CA")
                .setZipcode("94108")
                .build();

        final CardDetails expected = CardDetails.builder()
                .setCheckoutToken("TVY2I3JVYCW0FWYF")
                .setCvv("123")
                .setNumber("4111111111111111")
                .setCardholderName("AffirmInc John R Andrew")
                .setExpiration("0423")
                .setCallbackId("cd5d5440-3d63-4e4c-9aa2-fbf16ea689e6")
                .setId("TVY2I3JVYCW0FWYF")
                .setBillingAddress(billingAddress)
                .build();

        Mockito.verify(callbacks).onWebViewConfirmation(expected);
    }

    @Test
    public void shouldOverrideUrlLoading_Cancellation() {
        final String encodedData =
                "%7B%22reason%22%3A%22canceled%22%7D";
        affirmWebViewClient.shouldOverrideUrlLoading(webview,
                "affirm://checkout/cancelled?data=" + encodedData);

        final VcnReason expected = VcnReason.builder()
                .setReason("canceled")
                .build();
        Mockito.verify(callbacks).onWebViewCancellationReason(expected);
    }

    @Test
    public void shouldOverrideUrlLoading_Random() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview, "http://www.affirm.com/api/v1/get");
        Mockito.verify(callbacks, never()).onWebViewConfirmation(any(CardDetails.class));
        Mockito.verify(callbacks, never()).onWebViewCancellationReason(any(VcnReason.class));
    }

    @Test
    public void onReceivedError() {
        WebResourceRequest resourceRequest = Mockito.mock(WebResourceRequest.class);
        WebResourceError error = Mockito.mock(WebResourceError.class);
        when(error.getDescription()).thenReturn("error msg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            affirmWebViewClient.onReceivedError(webview, resourceRequest, error);
        } else {
            affirmWebViewClient.onReceivedError(webview, error.getErrorCode(),
                    error.getDescription() != null ? error.getDescription().toString() : "",
                    resourceRequest.getUrl() != null ? resourceRequest.getUrl().toString() : "");
        }

        affirmWebViewClient.onReceivedError(webview, resourceRequest, error);
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }
}

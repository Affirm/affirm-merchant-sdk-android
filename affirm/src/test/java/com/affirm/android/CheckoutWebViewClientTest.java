package com.affirm.android;

import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.affirm.android.exception.ConnectionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Uri.class)
public class CheckoutWebViewClientTest {

    @Mock
    CheckoutWebViewClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    CheckoutWebViewClient affirmWebViewClient;

    @Before
    public void setup() {
        mockUriParse();
    }

    private void mockUriParse() {
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.when(Uri.parse(anyString())).then((Answer<Uri>) invocation -> mockUri((String) invocation.getArguments()[0]));
    }

    private Uri mockUri(final String url) {
        Uri mockUri = mock(Uri.class);
        when(mockUri.toString()).thenReturn(url);
        when(mockUri.getQueryParameter("checkout_token")).then((Answer<String>) invocation -> url.substring(url.indexOf("checkout_token=") + "checkout_token=".length()));
        return mockUri;
    }

    @Test
    public void shouldOverrideUrlLoading_Confirmation() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview,
                "affirm://checkout/confirmed?checkout_token=123");
        Mockito.verify(callbacks).onWebViewConfirmation("123");
    }

    @Test
    public void shouldOverrideUrlLoading_Cancellation() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview, "affirm://checkout/cancelled");
        Mockito.verify(callbacks).onWebViewCancellation();
    }

    @Test
    public void shouldOverrideUrlLoading_Random() {
        affirmWebViewClient.shouldOverrideUrlLoading(webview, "http://www.affirm.com/api/v1/get");
        Mockito.verify(callbacks, never()).onWebViewConfirmation(any(String.class));
        Mockito.verify(callbacks, never()).onWebViewCancellation();
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
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }
}
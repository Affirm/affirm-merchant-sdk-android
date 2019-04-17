package com.affirm.android;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.affirm.android.exception.ConnectionException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckoutWebViewClientTest {

    @Mock
    CheckoutWebViewClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    CheckoutWebViewClient affirmWebViewClient;

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
        when(error.toString()).thenReturn("error msg");

        affirmWebViewClient.onReceivedError(webview, resourceRequest, error);
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }
}
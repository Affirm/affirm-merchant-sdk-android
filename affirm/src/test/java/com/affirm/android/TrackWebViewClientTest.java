package com.affirm.android;

import android.os.Build;
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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackWebViewClientTest {

    @Mock
    AffirmWebViewClient.WebViewClientCallbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    TrackWebViewClient trackWebViewClient;

    @Test
    public void onReceivedError() {
        WebResourceRequest resourceRequest = Mockito.mock(WebResourceRequest.class);
        WebResourceError error = Mockito.mock(WebResourceError.class);
        when(error.getDescription()).thenReturn("error msg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            trackWebViewClient.onReceivedError(webview, resourceRequest, error);
        } else {
            trackWebViewClient.onReceivedError(webview, error.getErrorCode(),
                    error.getDescription() != null ? error.getDescription().toString() : "",
                    resourceRequest.getUrl() != null ? resourceRequest.getUrl().toString() : "");
        }
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }
}
package com.affirm.android;

import android.os.Build;
import android.webkit.RenderProcessGoneDetail;
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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PromoWebViewClientTest {

    @Mock
    PromoWebViewClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    PromoWebViewClient promoWebViewClient;

    @Test
    public void onReceivedError() {
        WebResourceRequest resourceRequest = Mockito.mock(WebResourceRequest.class);
        WebResourceError error = Mockito.mock(WebResourceError.class);
        when(error.getDescription()).thenReturn("error msg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            promoWebViewClient.onReceivedError(webview, resourceRequest, error);
        } else {
            promoWebViewClient.onReceivedError(webview, error.getErrorCode(),
                    error.getDescription() != null ? error.getDescription().toString() : "",
                    resourceRequest.getUrl() != null ? resourceRequest.getUrl().toString() : "");
        }
        Mockito.verify(callbacks, never()).onWebViewError(new ConnectionException("error msg"));
    }

    @Test
    public void renderProcessGoneNotifiesRendererGone() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        RenderProcessGoneDetail detail = Mockito.mock(RenderProcessGoneDetail.class);
        when(detail.didCrash()).thenReturn(true);

        promoWebViewClient.onRenderProcessGone(webview, detail);

        Mockito.verify(callbacks).onRendererGone();
        Mockito.verify(callbacks, never()).onWebViewError(Mockito.any(ConnectionException.class));
    }

    @Test
    public void shouldOverrideUrlLoadingDoesNotNavigate() {
        boolean override = promoWebViewClient.shouldOverrideUrlLoading(
                webview, "https://www.affirm.com/apps/prequal");

        assertTrue(override);
    }

}

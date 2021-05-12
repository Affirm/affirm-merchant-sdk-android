package com.affirm.android;

import android.webkit.WebView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class AffirmWebChromeClientTest {

    @Mock
    AffirmWebChromeClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    AffirmWebChromeClient affirmWebChromeClient;

    @Test
    public void onProgressChangedTest() {
        affirmWebChromeClient.onProgressChanged(webview, 80);
        Mockito.verify(callbacks, never()).chromeLoadCompleted();
    }

    @Test
    public void onProgressChangedWithOneHundredTest() {
        affirmWebChromeClient.onProgressChanged(webview, 100);
        Mockito.verify(callbacks).chromeLoadCompleted();
    }
}
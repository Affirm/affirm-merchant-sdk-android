package com.affirm.android;

import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AffirmPromotionButtonTest {

    private AffirmPromotionButton button;
    private View.OnClickListener listener;

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }
        button = new AffirmPromotionButton(RuntimeEnvironment.getApplication());
        listener = mock(View.OnClickListener.class);
    }

    @Test
    public void listenerSetBeforeHtmlStylingReachesWebView() {
        button.setOnClickListener(listener);
        button.configWithHtmlStyling();
        button.setLabel("<p>Pay</p>", "Pay");

        PromotionWebView webView = button.getPromotionWebView();
        assertNotNull(webView);
        ready(webView);
        webView.performClick();

        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void listenerSetAfterHtmlStylingReachesWebView() {
        button.configWithHtmlStyling();
        button.setLabel("<p>Pay</p>", "Pay");
        button.setOnClickListener(listener);

        ready(button.getPromotionWebView());
        button.getPromotionWebView().performClick();

        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void clearedListenerIsNotInvoked() {
        button.configWithHtmlStyling();
        button.setLabel("<p>Pay</p>", "Pay");
        button.setOnClickListener(listener);
        button.setOnClickListener(null);

        ready(button.getPromotionWebView());
        button.getPromotionWebView().performClick();

        verify(listener, never()).onClick(any(View.class));
    }

    private static void ready(PromotionWebView webView) {
        webView.onScriptMessage("{\"event\":\"ready\",\"renderId\":\""
                + webView.renderId() + "\",\"height\":20}");
    }
}

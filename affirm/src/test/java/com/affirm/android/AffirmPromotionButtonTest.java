package com.affirm.android;

import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowWebView;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
        button = new AffirmPromotionButton(RuntimeEnvironment.getApplication());
        listener = mock(View.OnClickListener.class);
    }

    @Test
    public void listenerSetBeforeHtmlStylingReachesWebView() {
        button.setOnClickListener(listener);
        button.configWithHtmlStyling();

        PromotionWebView webView = button.getPromotionWebView();
        assertNotNull(webView);
        webView.performClick();

        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void webViewInsideTheButtonIsHiddenFromAccessibility() {
        button.configWithHtmlStyling();

        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                button.getPromotionWebView().getImportantForAccessibility());
    }

    @Test
    public void listenerSetAfterHtmlStylingReachesWebView() {
        button.configWithHtmlStyling();
        button.setOnClickListener(listener);

        button.getPromotionWebView().performClick();

        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void clearedListenerIsNotInvoked() {
        button.configWithHtmlStyling();
        button.setOnClickListener(listener);
        button.setOnClickListener(null);

        button.getPromotionWebView().performClick();

        verify(listener, never()).onClick(any(View.class));
    }

    @Test
    public void htmlPromoTakesTapsOnlyOnceReady() {
        initializeAffirm();
        button.configWithHtmlStyling();
        button.setOnClickListener(listener);
        button.setLabel("<span>a</span>", "Pay over time");

        button.performClick();
        verify(listener, never()).onClick(any(View.class));

        PromotionWebView webView = button.getPromotionWebView();
        webView.onScriptMessage(ready(webView));
        button.performClick();
        verify(listener, times(1)).onClick(button);
    }

    @Test
    public void localStylingTakesTapsRightAway() {
        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO);
        button.setOnClickListener(listener);
        button.setLabel("Pay over time", "Pay over time");

        button.performClick();

        verify(listener, times(1)).onClick(button);
    }

    @Test
    public void htmlPromoDescribesTheButtonForAccessibility() {
        initializeAffirm();
        button.configWithHtmlStyling();

        button.setLabel("<span>a</span>", "Pay over time");

        assertEquals("Pay over time", button.getContentDescription());
    }

    @Test
    public void rendererGoneReplacesTheWebViewForTheNextLabel() {
        initializeAffirm();
        button.configWithHtmlStyling();
        button.setLabel("<span>a</span>", "Pay over time");
        PromotionWebView crashed = button.getPromotionWebView();

        crashed.onRendererGone();

        PromotionWebView replacement = button.getPromotionWebView();
        assertNotSame(crashed, replacement);
        assertEquals(0, button.getChildCount());
        assertNull(lastLoad(replacement));
        assertTrue(button.isEmpty());
        assertNull(button.getContentDescription());

        button.setLabel("<span>a</span>", "Pay over time");

        assertEquals(1, button.getChildCount());
        assertNotNull(lastLoad(replacement));
        assertEquals("Pay over time", button.getContentDescription());
    }

    private static ShadowWebView.LoadDataWithBaseURL lastLoad(PromotionWebView webView) {
        ShadowWebView shadow = Shadow.extract(webView);
        return shadow.getLastLoadDataWithBaseURL();
    }

    private static String ready(PromotionWebView webView) {
        return "{\"event\":\"ready\",\"renderId\":\"" + webView.renderId() + "\",\"height\":20}";
    }

    private static void initializeAffirm() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }
    }
}

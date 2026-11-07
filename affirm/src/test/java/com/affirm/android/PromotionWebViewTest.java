package com.affirm.android;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowWebView;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class PromotionWebViewTest {

    private Context context;
    private PromotionWebView webView;
    private View.OnClickListener listener;
    private int dragDistance;

    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
        webView = new PromotionWebView(context);
        listener = mock(View.OnClickListener.class);
        dragDistance = ViewConfiguration.get(context).getScaledTouchSlop() * 4;
    }

    @Test
    public void loadWebDataUsesFileBaseUrl() {
        initializeAffirm();

        webView.loadWebData("Pay with <span class='__affirm-logo'>Affirm</span>", null, null);

        ShadowWebView shadow = Shadow.extract(webView);
        assertEquals(PromotionWebView.PROMO_BASE_URL,
                shadow.getLastLoadDataWithBaseURL().baseUrl);
    }

    @Test
    public void payloadHasThePageKeys() throws JSONException {
        initializeAffirm();

        webView.loadWebData("<span>a</span>", "https://cdn.test/a.css",
                "<link rel=\"stylesheet\" href=\"https://fonts.test/f.css\"><style>.f{}</style>");

        ShadowWebView shadow = Shadow.extract(webView);
        String page = shadow.getLastLoadDataWithBaseURL().data;
        String json = page.substring(page.indexOf("id=\"affirm-promo-payload\">")
                + "id=\"affirm-promo-payload\">".length());
        JSONObject payload = new JSONObject(json.substring(0, json.indexOf("</script>")));
        assertEquals(10, payload.length());
        assertEquals(webView.renderId(), payload.getString("renderId"));
        assertEquals("<span>a</span>", payload.getString("html"));
        assertEquals("https://cdn.test/a.css", payload.getString("remoteCssURL"));
        assertEquals("https://fonts.test/f.css", payload.getString("remoteFontURL"));
        assertTrue(payload.getString("jsURL").startsWith("https://"));
        assertTrue(payload.getString("bundledCSS").endsWith(".f{}"));
        assertEquals(5000, payload.getInt("timeoutMs"));
        for (String key : new String[]{"publicKey", "locale", "countryCode"}) {
            assertTrue(key, payload.has(key));
        }
    }

    @Test
    public void sameVisiblePromoDoesNotReload() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);
        String firstRender = webView.renderId();
        webView.loadWebData("<span>a</span>", null, null);
        assertNotEquals(firstRender, webView.renderId());

        webView.onScriptMessage(message("ready"));
        String shownRender = webView.renderId();
        webView.loadWebData("<span>a</span>", null, null);
        assertEquals(shownRender, webView.renderId());

        webView.loadWebData("<span>b</span>", null, null);
        assertNotEquals(shownRender, webView.renderId());
    }

    @Test
    public void scriptMessagesFromAnotherRenderAreIgnored() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);

        webView.onScriptMessage("{\"event\":\"ready\",\"renderId\":\"old\",\"height\":20}");
        assertEquals(View.INVISIBLE, webView.getVisibility());

        webView.onScriptMessage(message("ready"));
        assertEquals(View.VISIBLE, webView.getVisibility());
    }

    @Test
    public void emptySanitizedContentStaysHidden() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);
        String ready = message("ready");

        webView.onScriptMessage(message("failure"));
        webView.onScriptMessage(ready);

        assertEquals(View.GONE, webView.getVisibility());
        assertNull(webView.renderId());
    }

    @Test
    public void malformedScriptMessageIsIgnored() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);

        webView.onScriptMessage(null);
        webView.onScriptMessage("not json");

        assertEquals(View.INVISIBLE, webView.getVisibility());
    }

    @Test
    public void insecureStylesheetFailsClosed() {
        initializeAffirm();

        webView.loadWebData("<span>a</span>", null,
                "<link rel=\"stylesheet\" href=\"http://fonts.test/a.css\">");

        ShadowWebView shadow = Shadow.extract(webView);
        assertNull(shadow.getLastLoadDataWithBaseURL());
        assertEquals(View.GONE, webView.getVisibility());
    }

    @Test
    public void nativeTimeoutFailsWhenThePageNeverReports() {
        initializeAffirm();
        attachToWindow();
        webView.loadWebData("<span>a</span>", null, null);

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS - 1, TimeUnit.MILLISECONDS);
        assertEquals(View.INVISIBLE, webView.getVisibility());

        ShadowLooper.idleMainLooper(1, TimeUnit.MILLISECONDS);
        assertEquals(View.GONE, webView.getVisibility());
        assertNull(webView.renderId());
    }

    @Test
    public void readyCancelsTheNativeTimeout() {
        initializeAffirm();
        attachToWindow();
        webView.loadWebData("<span>a</span>", null, null);

        webView.onScriptMessage(message("ready"));
        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertEquals(View.VISIBLE, webView.getVisibility());
        assertTrue(webView.isRenderReady());
    }

    @Test
    public void rendererGoneDestroysTheWebView() {
        initializeAffirm();
        FrameLayout parent = new FrameLayout(context);
        parent.addView(webView);
        Runnable onDestroyed = mock(Runnable.class);
        webView.setOnDestroyedByRendererGone(onDestroyed);
        webView.loadWebData("<span>a</span>", null, null);

        webView.onRendererGone();

        ShadowWebView shadow = Shadow.extract(webView);
        assertTrue(shadow.wasDestroyCalled());
        assertEquals(0, parent.getChildCount());
        assertNull(webView.renderId());
        verify(onDestroyed).run();
    }

    @Test
    public void standaloneWebViewStaysAccessible() {
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO,
                webView.getImportantForAccessibility());
    }

    @Test
    public void tapInvokesListenerWithParent() {
        FrameLayout parent = new FrameLayout(context);
        parent.addView(webView);
        webView.setWebViewClickListener(listener);

        tap(SystemClock.uptimeMillis());

        verify(listener, times(1)).onClick(parent);
    }

    @Test
    public void tapWithoutParentInvokesListenerWithWebView() {
        webView.setWebViewClickListener(listener);

        tap(SystemClock.uptimeMillis());

        verify(listener, times(1)).onClick(webView);
    }

    @Test
    public void dragDoesNotInvokeListener() {
        webView.setWebViewClickListener(listener);
        long downTime = SystemClock.uptimeMillis();

        dispatch(downTime, downTime, MotionEvent.ACTION_DOWN, 10, 10);
        dispatch(downTime, downTime + 50, MotionEvent.ACTION_MOVE, 10 + dragDistance, 10);
        dispatch(downTime, downTime + 100, MotionEvent.ACTION_UP, 10 + dragDistance, 10);

        verify(listener, never()).onClick(any(View.class));
    }

    @Test
    public void cancelledGestureDoesNotInvokeListener() {
        webView.setWebViewClickListener(listener);
        long downTime = SystemClock.uptimeMillis();

        dispatch(downTime, downTime, MotionEvent.ACTION_DOWN, 10, 10);
        dispatch(downTime, downTime + 50, MotionEvent.ACTION_CANCEL, 10, 10);

        verify(listener, never()).onClick(any(View.class));
    }

    @Test
    public void separateTapsEachInvokeListener() {
        webView.setWebViewClickListener(listener);
        long gap = ViewConfiguration.getDoubleTapTimeout() + 100;
        long start = SystemClock.uptimeMillis();

        tap(start);
        tap(start + gap);

        verify(listener, times(2)).onClick(any(View.class));
    }

    @Test
    public void listenerCanBeCleared() {
        webView.setWebViewClickListener(listener);
        webView.setWebViewClickListener(null);

        tap(SystemClock.uptimeMillis());

        verify(listener, never()).onClick(any(View.class));
    }

    @Test
    public void touchEventsAreConsumedWithoutListener() {
        long downTime = SystemClock.uptimeMillis();

        assertTrue(dispatch(downTime, downTime, MotionEvent.ACTION_DOWN, 10, 10));
        assertTrue(dispatch(downTime, downTime + 50, MotionEvent.ACTION_UP, 10, 10));
    }

    @Test
    public void performClickInvokesListener() {
        webView.setWebViewClickListener(listener);

        webView.performClick();

        verify(listener, times(1)).onClick(webView);
    }

    private String message(String event) {
        return "{\"event\":\"" + event + "\",\"renderId\":\"" + webView.renderId()
                + "\",\"height\":20,\"reason\":\"html_empty\"}";
    }

    private void attachToWindow() {
        Robolectric.buildActivity(Activity.class).setup().get().setContentView(webView);
    }

    private static void initializeAffirm() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }
    }

    private void tap(long downTime) {
        dispatch(downTime, downTime, MotionEvent.ACTION_DOWN, 10, 10);
        dispatch(downTime, downTime + 50, MotionEvent.ACTION_UP, 10, 10);
    }

    private boolean dispatch(long downTime, long eventTime, int action, float x, float y) {
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0);
        try {
            return webView.dispatchTouchEvent(event);
        } finally {
            event.recycle();
        }
    }
}

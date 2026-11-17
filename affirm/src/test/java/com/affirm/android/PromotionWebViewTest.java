package com.affirm.android;

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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowWebView;

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
    private int touchSlop;
    private int dragDistance;

    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
        webView = new PromotionWebView(context);
        listener = mock(View.OnClickListener.class);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        dragDistance = touchSlop * 4;
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
    public void loadedPageRendersTheNextPromoWithoutReloading() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);
        ShadowWebView shadow = Shadow.extract(webView);
        ShadowWebView.LoadDataWithBaseURL page = shadow.getLastLoadDataWithBaseURL();
        webView.onScriptMessage(message("ready"));

        webView.loadWebData("<span>b</span>", null, null);

        assertEquals(page, shadow.getLastLoadDataWithBaseURL());
        String script = shadow.getLastEvaluatedJavascript();
        assertTrue(script, script.startsWith("window.affirmPromoRender({"));
        assertTrue(script, script.contains(webView.renderId()));
        assertTrue(script, script.contains("\\u003cspan\\u003eb\\u003c/span\\u003e"));
        assertEquals(0f, webView.getAlpha(), 0f);
    }

    @Test
    public void promoForALoadingPageRendersOnceThePageAnswers() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);
        String first = webView.renderId();
        webView.loadWebData("<span>b</span>", null, null);
        ShadowWebView shadow = Shadow.extract(webView);
        assertNull(shadow.getLastEvaluatedJavascript());

        webView.onScriptMessage("{\"event\":\"sanitized\",\"usable\":true,\"renderId\":\""
                + first + "\"}");

        assertTrue(shadow.getLastEvaluatedJavascript().contains(webView.renderId()));
    }

    @Test
    public void replacedPageDoesNotCountAsLoaded() {
        initializeAffirm();
        ShadowWebView shadow = Shadow.extract(webView);
        webView.loadWebData("<span>a</span>", null, null);
        String replacedPage = webView.renderId();
        webView.loadWebData("<span>a</span>", null, "<style>p{color:red}</style>");
        webView.loadWebData("<span>b</span>", null, "<style>p{color:red}</style>");

        webView.onScriptMessage("{\"event\":\"sanitized\",\"usable\":true,\"renderId\":\""
                + replacedPage + "\"}");

        assertNull(shadow.getLastEvaluatedJavascript());
    }

    @Test
    public void otherStyleOrAFailureLoadsANewPage() {
        initializeAffirm();
        ShadowWebView shadow = Shadow.extract(webView);
        webView.loadWebData("<span>a</span>", null, null);
        webView.onScriptMessage(message("ready"));

        webView.loadWebData("<span>a</span>", null, "<style>p{color:red}</style>");
        ShadowWebView.LoadDataWithBaseURL restyled = shadow.getLastLoadDataWithBaseURL();
        assertTrue(restyled.data.contains("p{color:red}"));
        webView.onScriptMessage(message("failure"));

        webView.loadWebData("<span>b</span>", null, "<style>p{color:red}</style>");
        assertNotEquals(restyled, shadow.getLastLoadDataWithBaseURL());
        assertNull(shadow.getLastEvaluatedJavascript());
    }

    @Test
    public void hiddenPromoIgnoresThePage() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);
        String ready = message("ready");

        webView.hidePromo();
        webView.onScriptMessage(ready);

        assertEquals(View.GONE, webView.getVisibility());
        assertNull(webView.renderId());
    }

    @Test
    public void scriptMessagesFromAnotherRenderAreIgnored() {
        initializeAffirm();
        webView.loadWebData("<span>a</span>", null, null);

        webView.onScriptMessage("{\"event\":\"ready\",\"renderId\":\"old\",\"height\":20}");
        assertEquals(View.VISIBLE, webView.getVisibility());
        assertEquals(0f, webView.getAlpha(), 0f);

        webView.onScriptMessage(message("ready"));
        assertEquals(View.VISIBLE, webView.getVisibility());
        assertEquals(1f, webView.getAlpha(), 0f);
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

        assertEquals(View.VISIBLE, webView.getVisibility());
        assertEquals(0f, webView.getAlpha(), 0f);
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
    public void tapWithSmallMovementInvokesListenerOnce() {
        webView.setWebViewClickListener(listener);
        long downTime = SystemClock.uptimeMillis();
        int jitter = touchSlop - 1;

        dispatch(downTime, downTime, MotionEvent.ACTION_DOWN, 10, 10);
        dispatch(downTime, downTime + 25, MotionEvent.ACTION_MOVE, 10 + jitter, 10);
        dispatch(downTime, downTime + 50, MotionEvent.ACTION_UP, 10 + jitter, 10);

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
    public void tapOnALoadingPromoIsIgnored() {
        initializeAffirm();
        webView.setWebViewClickListener(listener);
        webView.loadWebData("<span>a</span>", null, null);

        tap(SystemClock.uptimeMillis());
        verify(listener, never()).onClick(any(View.class));

        webView.onScriptMessage(message("ready"));
        tap(SystemClock.uptimeMillis() + ViewConfiguration.getDoubleTapTimeout() + 100);
        verify(listener, times(1)).onClick(any(View.class));
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

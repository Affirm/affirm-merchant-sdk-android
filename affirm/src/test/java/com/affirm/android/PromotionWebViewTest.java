package com.affirm.android;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowWebView;

import static org.junit.Assert.assertEquals;
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
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }

        webView.loadWebData("Pay with <span class='__affirm-logo'>Affirm</span>", null, null);

        ShadowWebView shadow = Shadow.extract(webView);
        assertEquals(PromotionWebView.PROMO_BASE_URL,
                shadow.getLastLoadDataWithBaseURL().baseUrl);
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
    public void performClickInvokesListener() {
        webView.setWebViewClickListener(listener);

        webView.performClick();

        verify(listener, times(1)).onClick(webView);
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

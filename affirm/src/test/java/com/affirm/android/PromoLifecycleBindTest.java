package com.affirm.android;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PromoLifecycleBindTest {

    @Test
    public void cancelsWhenActivityDestroyed() {
        TestLifecycleActivity activity =
                Robolectric.buildActivity(TestLifecycleActivity.class).get();
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        View view = mock(View.class);
        when(view.getContext()).thenReturn(activity);
        PromoRequest request = mock(PromoRequest.class);

        Affirm.bindPromoRequestToLifecycle(view, request);

        verify(request, never()).cancel();
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        verify(request).cancel();
    }

    @Test
    public void cancelsIfActivityAlreadyDestroyed() {
        TestLifecycleActivity activity = mock(TestLifecycleActivity.class);
        when(activity.isDestroyed()).thenReturn(true);
        View view = mock(View.class);
        when(view.getContext()).thenReturn(activity);
        PromoRequest request = mock(PromoRequest.class);

        Affirm.bindPromoRequestToLifecycle(view, request);

        verify(request).cancel();
    }

    @Test
    public void cancelsIfActivityFinishing() {
        TestLifecycleActivity activity = mock(TestLifecycleActivity.class);
        when(activity.isFinishing()).thenReturn(true);
        View view = mock(View.class);
        when(view.getContext()).thenReturn(activity);
        PromoRequest request = mock(PromoRequest.class);

        Affirm.bindPromoRequestToLifecycle(view, request);

        verify(request).cancel();
    }

    @Test
    public void doesNotBindRawActivity() {
        ActivityController<Activity> controller =
                Robolectric.buildActivity(Activity.class).setup();
        PromoRequest request = mock(PromoRequest.class);

        View view = mock(View.class);
        when(view.getContext()).thenReturn(controller.get());
        Affirm.bindPromoRequestToLifecycle(view, request);

        verify(request, never()).cancel();
        controller.destroy();
        verify(request, never()).cancel();
    }

    public static class TestLifecycleActivity extends Activity implements LifecycleOwner {
        final LifecycleRegistry registry = new LifecycleRegistry(this);

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }
}

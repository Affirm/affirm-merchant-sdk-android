package com.affirm.android;

import android.annotation.SuppressLint;
import android.app.Fragment;

import androidx.annotation.NonNull;

public class LifeListenerFragment extends Fragment {
    private final ActivityFragmentLifecycle lifecycle;
    private boolean isStarted;
    private boolean isDestroyed;

    public LifeListenerFragment() {
        this(new ActivityFragmentLifecycle());
    }

    @SuppressLint("ValidFragment")
    public LifeListenerFragment(ActivityFragmentLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void addLifeListener(@NonNull LifecycleListener listener) {
        lifecycle.addListener(listener);

        if (isDestroyed) {
            listener.onDestroy();
        } else if (isStarted) {
            listener.onStart();
        } else {
            listener.onStop();
        }
    }

    public void removeLifeListener(@NonNull LifecycleListener listener) {
        lifecycle.removeListener(listener);
    }

    @Override
    public void onStart() {
        super.onStart();
        isStarted = true;
        lifecycle.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        isStarted = false;
        lifecycle.onStop();
    }

    @Override
    public void onDestroy() {
        isDestroyed = true;
        lifecycle.onDestroy();
        lifecycle.clearListener();
        super.onDestroy();
    }
}

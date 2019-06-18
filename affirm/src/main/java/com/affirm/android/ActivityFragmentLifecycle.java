package com.affirm.android;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class ActivityFragmentLifecycle implements Lifecycle {
    private final Set<LifecycleListener> lifecycleListeners =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    void clearListener() {
        lifecycleListeners.clear();
    }

    void onStart() {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStart();
        }
    }

    void onStop() {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onStop();
        }
    }

    void onDestroy() {
        for (LifecycleListener lifecycleListener : lifecycleListeners) {
            lifecycleListener.onDestroy();
        }
    }
}


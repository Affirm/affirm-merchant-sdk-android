package com.affirm.android;

import android.app.Fragment;

public class LifeListenerFragment extends Fragment {

    private LifecycleListener mLifeListener;

    void addLifeListener(LifecycleListener listener) {
        mLifeListener = listener;
    }

    void removeLifeListener() {
        mLifeListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mLifeListener != null) {
            mLifeListener.onStart();
        }
    }

    @Override
    public void onDestroy() {
        if (mLifeListener != null) {
            mLifeListener.onStop();
        }
        super.onDestroy();
    }
}

package com.affirm.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import static com.affirm.android.AffirmConstants.AFFIRM_NOT_INITIALIZED_MESSAGE;

abstract class AffirmFragment extends Fragment implements AffirmWebChromeClient.Callbacks {

    protected static final String TAG_PREFIX = "AffirmFragment";

    AffirmWebView webView;
    private View progressIndicator;

    abstract void initViews();

    abstract void onAttached();

    abstract void onNotInitialized(@NonNull String message);

    protected static void addFragment(FragmentManager fragmentManager, @IdRes int containerViewId,
                                      @NonNull Fragment fragment, @NonNull String tag) {
        fragmentManager
                .beginTransaction()
                .add(containerViewId, fragment, tag)
                .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();
    }

    protected void removeFragment(@NonNull String tag) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) {
            AffirmLog.d("The fragment is getting detached from the Activity");
            return;
        }
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment != null) {
            fragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AffirmPlugins.restoreIfNeeded(savedInstanceState);
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.affirm_fragment_webview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (AffirmPlugins.get() == null) {
            AffirmLog.e(AFFIRM_NOT_INITIALIZED_MESSAGE);
            if (getTag() != null) {
                removeFragment(getTag());
            }
            onNotInitialized(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        webView = view.findViewById(R.id.webview);
        progressIndicator = view.findViewById(R.id.progressIndicator);

        AffirmUtils.debuggableWebView(getContext());
        initViews();
        onAttached();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        AffirmPlugins.saveState(outState);
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            webView.destroyWebView();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }
}

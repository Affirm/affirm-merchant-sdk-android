package com.affirm.android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public abstract class AffirmFragment extends Fragment implements AffirmWebChromeClient.Callbacks {

    protected static final String TAG_PREFIX = "AffirmFragment";

    protected WebView webView;
    private View progressIndicator;

    abstract void initViews();

    abstract void onAttached();

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
            fragmentManager.beginTransaction().remove(fragment).commit();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
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

        webView = view.findViewById(R.id.webview);
        progressIndicator = view.findViewById(R.id.progressIndicator);

        AffirmUtils.debuggableWebView(getActivity());
        initViews();
        onAttached();
    }

    @Override
    public void onDestroy() {
        if (getActivity() != null) {
            ViewGroup container =
                    getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
            container.removeView(webView);
        }
        webView.removeAllViews();
        webView.clearCache(true);
        webView.destroyDrawingCache();
        webView.clearHistory();
        webView.destroy();
        webView = null;
        super.onDestroy();
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }
}

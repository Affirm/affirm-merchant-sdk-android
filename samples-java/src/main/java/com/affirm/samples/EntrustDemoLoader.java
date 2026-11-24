package com.affirm.samples;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.CheckoutActivity;

/**
 * Test only: once the SDK's checkout activity has loaded checkout, opens the Entrust (Onfido) demo
 * in a frame of its WebView, so the SDK answers the demo's camera and microphone requests. The SDK
 * does not expose its WebView, so it is looked up by the SDK's view id.
 */
final class EntrustDemoLoader implements Application.ActivityLifecycleCallbacks {

    private static final long TIMEOUT_MS = 30_000;
    private static final long POLL_MS = 500;

    interface Listener {
        void onEntrustDemoLoaded(@NonNull String url);

        void onEntrustDemoTimeout();
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private boolean armed;
    private String url;
    private long deadline;
    @Nullable
    private Activity checkoutActivity;

    EntrustDemoLoader(@NonNull Listener listener) {
        this.listener = listener;
    }

    /** Opens the demo at this URL in the next checkout activity that loads checkout. */
    void arm(@NonNull String url) {
        this.url = url;
        armed = true;
        deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
    }

    void disarm() {
        armed = false;
        handler.removeCallbacks(poll);
    }

    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            if (!armed || checkoutActivity == null) {
                return;
            }
            final WebView webView = checkoutActivity.findViewById(com.affirm.android.R.id.webview);
            final String current = webView == null ? null : webView.getUrl();
            // The SDK loads the checkout URL once the checkout is created; a page loaded earlier
            // would be replaced.
            if (current != null && current.startsWith("https://")) {
                armed = false;
                LogPanel.mark("Open " + url);
                // The SDK only answers media requests on uk.affirm.com pages, so the demo runs in a
                // frame of a page with the checkout's origin, as Entrust runs inside checkout. It
                // needs a UK public key.
                final String base = "https://" + Uri.parse(current).getHost();
                webView.loadDataWithBaseURL(base, frameHtml(url), "text/html", "UTF-8", base);
                listener.onEntrustDemoLoaded(url);
            } else if (SystemClock.elapsedRealtime() < deadline) {
                handler.postDelayed(this, POLL_MS);
            } else {
                armed = false;
                listener.onEntrustDemoTimeout();
            }
        }
    };

    @NonNull
    private static String frameHtml(@NonNull String url) {
        return "<!DOCTYPE html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>html,body,iframe{margin:0;border:0;width:100%;height:100%}</style>"
                + "</head><body><iframe src=\"" + url + "\" allow=\"camera; microphone\">"
                + "</iframe></body></html>";
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (armed && activity instanceof CheckoutActivity) {
            checkoutActivity = activity;
            handler.post(poll);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity == checkoutActivity) {
            handler.removeCallbacks(poll);
            checkoutActivity = null;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity,
                                            @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }
}

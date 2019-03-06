package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

abstract class CheckoutCommonActivity extends AppCompatActivity implements AffirmWebViewClient.Callbacks, AffirmWebChromeClient.Callbacks {

    static final int RESULT_ERROR = -8575;

    static final String CHECKOUT_ERROR = "checkout_error";

    static final String CHECKOUT_EXTRA = "checkout_extra";

    private AsyncTask task;

    Checkout checkout;

    ViewGroup container;
    WebView webView;
    View progressIndicator;

    final TaskCreator taskCreator = new TaskCreator() {
        @Override
        public void create(@NonNull Context context, @NonNull Checkout checkout, @Nullable CheckoutCallback callback) {
            executeTask(null, new CheckoutTask(context, checkout, callback));
        }

        @Override
        public void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
                task = null;
            }
        }
    };

    abstract void startCheckout();

    abstract void setupWebView();

    abstract CheckoutResponse executeTask(Checkout checkout) throws IOException;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.hideActionBar(this);

        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }

        setContentView(R.layout.activity_webview);
        container = findViewById(R.id.container);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        setupWebView();

        startCheckout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
    }

    @Override
    protected void onDestroy() {
        taskCreator.cancel();
        clearCookies();
        container.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }

    public void clearCookies() {
        final CookieManager cookieManager = CookieManager.getInstance();
        final CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
        CookiesUtil.clearCookieByUrl("https://" + AffirmPlugins.get().baseUrl(), cookieManager, cookieSyncManager);
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
    }

    @Override
    public void onWebViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onWebViewPageLoaded() {
        // ignore this
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }

    interface CheckoutCallback {

        void onError(Exception exception);

        void onSuccess(CheckoutResponse response);
    }


    interface TaskCreator {
        void create(
                @NonNull final Context context,
                @NonNull final Checkout checkout,
                @Nullable final CheckoutCallback callback);

        void cancel();
    }

    void executeTask(@Nullable Executor executor,
                     @NonNull AsyncTask<Void, Void, CheckoutResponseWrapper> task) {
        this.task = task;
        if (executor != null) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }

    private static class CheckoutTask extends AsyncTask<Void, Void, CheckoutResponseWrapper> {
        @NonNull
        private final Checkout checkout;
        @NonNull
        private final WeakReference<CheckoutCallback> mCallbackRef;

        @NonNull
        private final WeakReference<Context> mContextRef;

        CheckoutTask(@NonNull Context context,
                     @NonNull final Checkout checkout,
                     @Nullable final CheckoutCallback callback) {
            this.mContextRef = new WeakReference<>(context);
            this.checkout = checkout;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected CheckoutResponseWrapper doInBackground(Void... params) {
            if (mContextRef.get() != null && mContextRef.get() instanceof CheckoutCommonActivity) {
                try {
                    return new CheckoutResponseWrapper(((CheckoutCommonActivity) mContextRef.get()).executeTask(checkout));
                } catch (IOException e) {
                    return new CheckoutResponseWrapper(e);
                }
            } else {
                return new CheckoutResponseWrapper(new Exception());
            }
        }

        @Override
        protected void onPostExecute(CheckoutResponseWrapper result) {
            final CheckoutCallback checkoutCallback = mCallbackRef.get();
            if (checkoutCallback != null) {
                if (result.source != null) {
                    checkoutCallback.onSuccess(result.source);
                } else if (result.error != null) {
                    checkoutCallback.onError(result.error);
                }
            }
        }
    }

    private static class CheckoutResponseWrapper {

        CheckoutResponse source;

        Exception error;

        CheckoutResponseWrapper(CheckoutResponse source) {
            this.source = source;
        }

        CheckoutResponseWrapper(Exception error) {
            this.error = error;
        }
    }
}

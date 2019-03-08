package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class CheckoutBaseActivity extends AffirmActivity implements AffirmWebViewClient.Callbacks {

    static final int RESULT_ERROR = -8575;

    static final String CHECKOUT_ERROR = "checkout_error";

    static final String CHECKOUT_EXTRA = "checkout_extra";

    private AsyncTask checkoutTask;

    Checkout checkout;

    abstract CheckoutResponse executeTask(Checkout checkout) throws IOException;

    final CheckoutTaskCreator taskCreator = new CheckoutTaskCreator() {
        @Override
        public void create(@NonNull Context context, @NonNull Checkout checkout,
                           @Nullable CheckoutCallback callback) {
            executeTask(null, new CheckoutTask(context, checkout, callback));
        }

        @Override
        public void cancel() {
            if (checkoutTask != null && !checkoutTask.isCancelled()) {
                checkoutTask.cancel(true);
                checkoutTask = null;
            }
        }
    };

    @Override
    void beforeOnCreate() {
        AffirmUtils.hideActionBar(this);
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
    }

    @Override
    protected void onDestroy() {
        taskCreator.cancel();
        super.onDestroy();
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

    interface CheckoutCallback {

        void onError(Exception exception);

        void onSuccess(CheckoutResponse response);
    }

    void executeTask(@Nullable Executor executor,
                     @NonNull AsyncTask<Void, Void, ResponseWrapper<CheckoutResponse>> task) {
        this.checkoutTask = task;
        if (executor != null) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }

    private static class CheckoutTask extends AsyncTask<Void, Void,
            ResponseWrapper<CheckoutResponse>> {
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
        protected ResponseWrapper<CheckoutResponse> doInBackground(Void... params) {
            if (mContextRef.get() != null && mContextRef.get() instanceof CheckoutBaseActivity) {
                try {
                    CheckoutBaseActivity checkoutBaseActivity =
                            (CheckoutBaseActivity) mContextRef.get();
                    CheckoutResponse checkoutResponse = checkoutBaseActivity.executeTask(checkout);
                    return new ResponseWrapper<>(checkoutResponse);
                } catch (IOException e) {
                    return new ResponseWrapper<>(e);
                }
            } else {
                return new ResponseWrapper<>(new Exception());
            }
        }

        @Override
        protected void onPostExecute(ResponseWrapper<CheckoutResponse> result) {
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
}

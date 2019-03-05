package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class CheckoutActivity extends CheckoutCommonActivity implements CheckoutWebViewClient.Callbacks {

    public static final String CHECKOUT_TOKEN = "checkout_token";

    static void startActivity(@NonNull Activity activity, int requestCode, @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, CheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void startCheckout() {
        new CheckoutTask(checkout, new CheckoutCallback() {
            @Override
            public void onError(Exception exception) {
                onWebViewError(exception);
            }

            @Override
            public void onSuccess(CheckoutResponse response) {
                webView.loadUrl(response.redirectUrl());
            }
        }).execute();
    }

    @Override
    void setupWebView() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new CheckoutWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    public void onWebViewConfirmation(@NonNull String token) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_TOKEN, token);
        setResult(RESULT_OK, intent);
        finish();
    }

    private static class CheckoutTask extends AsyncTask<Void, Void, CheckoutResponseWrapper> {
        @NonNull
        private final Checkout checkout;
        @NonNull
        private final WeakReference<CheckoutCallback> mCallbackRef;

        CheckoutTask(@NonNull final Checkout checkout,
                     @Nullable final CheckoutCallback callback) {
            this.checkout = checkout;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected CheckoutResponseWrapper doInBackground(Void... params) {
            try {
                return new CheckoutResponseWrapper(AffirmApiHandler.executeCheckout(checkout), null);
            } catch (IOException e) {
                return new CheckoutResponseWrapper(null, e);
            }
        }

        @Override
        protected void onPostExecute(CheckoutResponseWrapper result) {
            if (result.response != null && mCallbackRef.get() != null) {
                mCallbackRef.get().onSuccess(result.response);
            } else {
                mCallbackRef.get().onError(result.exception);
            }
        }
    }
}

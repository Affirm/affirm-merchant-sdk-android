package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;

import androidx.annotation.NonNull;

class CheckoutActivity extends CheckoutCommonActivity implements CheckoutWebViewClient.Callbacks {

    public static final String CHECKOUT_TOKEN = "checkout_token";

    static void startActivity(@NonNull Activity activity, int requestCode, @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, CheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void startCheckout() {
        CheckoutCallback checkoutCallback = new CheckoutCallback() {
            @Override
            public void onError(Exception exception) {
                onWebViewError(exception);
            }

            @Override
            public void onSuccess(CheckoutResponse response) {
                webView.loadUrl(response.redirectUrl());
            }
        };

        taskCreator.create(this, checkout, checkoutCallback);
    }

    @Override
    void setupWebView() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new CheckoutWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    CheckoutResponse executeTask(Checkout checkout) throws IOException {
        return AffirmApiHandler.executeCheckout(checkout);
    }

    @Override
    public void onWebViewConfirmation(@NonNull String token) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_TOKEN, token);
        setResult(RESULT_OK, intent);
        finish();
    }
}

package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

class CheckoutActivity extends CheckoutBaseActivity implements CheckoutWebViewClient.Callbacks {

    public static final String CHECKOUT_TOKEN = "checkout_token";

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, CheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new CheckoutWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void onAttached() {
        CheckoutCallback checkoutCallback = new CheckoutCallback() {
            @Override
            public void onError(Exception exception) {
                AffirmTracker.track(CHECKOUT_CREATION_FAIL, ERROR, null);
                finishWithError(exception);
            }

            @Override
            public void onSuccess(CheckoutResponse response) {
                AffirmTracker.track(CHECKOUT_CREATION_SUCCESS, INFO, null);
                webView.loadUrl(response.redirectUrl());
            }
        };

        taskCreator.create(this, checkout, checkoutCallback);
    }

    @Override
    CheckoutResponse executeTask(Checkout checkout) throws IOException, APIException, PermissionException, InvalidRequestException {
        return AffirmApiHandler.executeCheckout(checkout);
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        AffirmTracker.track(CHECKOUT_WEBVIEW_FAIL, ERROR, null);
        finishWithError(error);
    }

    @Override
    public void onWebViewConfirmation(@NonNull String token) {
        AffirmTracker.track(CHECKOUT_WEBVIEW_SUCCESS, INFO, null);

        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_TOKEN, token);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onWebViewCancellation() {
        super.onWebViewCancellation();
    }

    @Override
    protected void onDestroy() {
        AffirmApiHandler.cancelCheckoutCall();
        super.onDestroy();
    }
}

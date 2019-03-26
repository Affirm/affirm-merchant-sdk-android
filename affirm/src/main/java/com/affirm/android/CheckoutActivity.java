package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.CheckoutRequest.CheckoutType;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;
import static com.affirm.android.Constants.CHECKOUT_EXTRA;
import static com.affirm.android.Constants.CHECKOUT_TOKEN;

public class CheckoutActivity extends CheckoutCommonActivity
    implements CheckoutWebViewClient.Callbacks {

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
    CheckoutType getCheckoutType() {
        return CheckoutType.REGULAR;
    }

    @Override
    CheckoutResponse executeTask(@NonNull Checkout checkout) throws APIException,
        PermissionException, InvalidRequestException, ConnectionException {
        return AffirmApiHandler.executeCheckout(checkout);
    }

    @Override
    InnerCheckoutCallback getInnerCheckoutCallback() {
        return new InnerCheckoutCallback() {
            @Override
            public void onError(@NonNull AffirmException exception) {
                AffirmTracker.track(CHECKOUT_CREATION_FAIL, ERROR, null);
                finishWithError(exception);
            }

            @Override
            public void onSuccess(@NonNull CheckoutResponse response) {
                AffirmTracker.track(CHECKOUT_CREATION_SUCCESS, INFO, null);
                webView.loadUrl(response.redirectUrl());
            }
        };
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
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
        webViewCancellation();
    }
}

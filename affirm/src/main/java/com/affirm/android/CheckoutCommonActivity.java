package com.affirm.android;

import android.content.Intent;
import android.os.Bundle;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

abstract class CheckoutCommonActivity extends AffirmActivity {

    static final int RESULT_ERROR = -8575;

    static final String CHECKOUT_ERROR = "checkout_error";

    static final String CHECKOUT_EXTRA = "checkout_extra";

    CheckoutRequest checkoutRequest;

    Checkout checkout;

    CheckoutRequest.CheckoutType checkoutType;

    abstract CheckoutResponse executeTask(@NonNull Checkout checkout)
            throws APIException, PermissionException, InvalidRequestException, ConnectionException;

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
        checkoutRequest.cancel();
        super.onDestroy();
    }

    protected void finishWithError(@NonNull Throwable error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
    }

    protected void webViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
    }

}

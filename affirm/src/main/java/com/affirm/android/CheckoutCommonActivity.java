package com.affirm.android;

import android.content.Intent;
import android.os.Bundle;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.CheckoutRequest.CheckoutType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.affirm.android.Constants.CHECKOUT_ERROR;
import static com.affirm.android.Constants.CHECKOUT_EXTRA;

abstract class CheckoutCommonActivity extends AffirmActivity {

    private CheckoutRequest mCheckoutRequest;

    private Checkout mCheckout;

    abstract CheckoutType checkoutType();

    abstract CheckoutResponse executeTask(@NonNull Checkout checkout)
        throws APIException, PermissionException, InvalidRequestException, ConnectionException;

    abstract InnerCheckoutCallback innerCheckoutCallback();

    @Override
    void beforeOnCreate() {
        AffirmUtils.hideActionBar(this);
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCheckout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            mCheckout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, mCheckout);
    }

    @Override
    void onAttached() {
        mCheckoutRequest = new CheckoutRequest(this, mCheckout,
            innerCheckoutCallback(), checkoutType());
        mCheckoutRequest.create();
    }

    @Override
    protected void onDestroy() {
        mCheckoutRequest.cancel();
        super.onDestroy();
    }

    protected void finishWithError(@NonNull Throwable error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error.toString());
        setResult(Affirm.RESULT_ERROR, intent);
        finish();
    }

    protected void webViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
    }

}

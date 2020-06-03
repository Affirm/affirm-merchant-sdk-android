package com.affirm.android;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

abstract class CheckoutBaseFragment extends AffirmFragment {

    private CheckoutRequest checkoutRequest;

    private Checkout checkout;

    abstract InnerCheckoutCallback innerCheckoutCallback();

    abstract boolean useVCN();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.requireNonNull(getArguments(), "mArguments cannot be null");
        checkout = getArguments().getParcelable(CHECKOUT_EXTRA);
    }

    @Override
    void onAttached() {
        checkoutRequest = new CheckoutRequest(checkout, innerCheckoutCallback(), useVCN());
        checkoutRequest.create();
    }

    @Override
    public void onDestroy() {
        checkoutRequest.cancel();
        super.onDestroy();
    }
}

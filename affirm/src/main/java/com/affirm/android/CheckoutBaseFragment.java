package com.affirm.android;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

abstract class CheckoutBaseFragment extends AffirmFragment {

    private CheckoutRequest checkoutRequest;

    private Checkout checkout;

    private String caas;

    private int cardAuthWindow;

    abstract InnerCheckoutCallback innerCheckoutCallback();

    abstract boolean useVCN();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.requireNonNull(getArguments(), "mArguments cannot be null");
        checkout = getArguments().getParcelable(CHECKOUT_EXTRA);
        caas = getArguments().getString(CHECKOUT_CAAS_EXTRA);
        cardAuthWindow = getArguments().getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
    }

    @Override
    void onAttached() {
        checkoutRequest = new CheckoutRequest(checkout, innerCheckoutCallback(), caas, useVCN(),
                cardAuthWindow);
        checkoutRequest.create();
    }

    @Override
    public void onDestroy() {
        checkoutRequest.cancel();
        super.onDestroy();
    }
}

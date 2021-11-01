package com.affirm.android;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

abstract class CheckoutBaseActivity extends AffirmActivity {

    private Checkout checkout;

    private String caas;

    private int cardAuthWindow;

    abstract boolean useVCN();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Hide the actionbar because the Affirm screen is already included
        AffirmUtils.hideActionBar(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
            caas = savedInstanceState.getString(CHECKOUT_CAAS_EXTRA);
            cardAuthWindow = savedInstanceState.getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
            caas = getIntent().getStringExtra(CHECKOUT_CAAS_EXTRA);
            cardAuthWindow = getIntent().getIntExtra(CHECKOUT_CARD_AUTH_WINDOW, -1);
        }

        Affirm.startCheckout(this, android.R.id.content, checkout, caas, cardAuthWindow,
                useVCN());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CHECKOUT_EXTRA, checkout);
        outState.putString(CHECKOUT_CAAS_EXTRA, caas);
        outState.putInt(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
    }
}

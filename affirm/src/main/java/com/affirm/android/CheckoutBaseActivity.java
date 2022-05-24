package com.affirm.android;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

import org.joda.money.Money;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.NEW_FLOW;

abstract class CheckoutBaseActivity extends AffirmActivity {

    protected Checkout checkout;

    private Money money;

    protected boolean newFlow;

    protected String caas;

    private int cardAuthWindow;

    abstract boolean useVCN();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
            caas = savedInstanceState.getString(CHECKOUT_CAAS_EXTRA);
            money = (Money) savedInstanceState.getSerializable(CHECKOUT_MONEY);
            newFlow = savedInstanceState.getBoolean(NEW_FLOW);
            cardAuthWindow = savedInstanceState.getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
            caas = getIntent().getStringExtra(CHECKOUT_CAAS_EXTRA);
            money = (Money) getIntent().getSerializableExtra(CHECKOUT_MONEY);
            newFlow = getIntent().getBooleanExtra(NEW_FLOW, false);
            cardAuthWindow = getIntent().getIntExtra(CHECKOUT_CARD_AUTH_WINDOW, -1);
        }

        Affirm.startCheckout(this, android.R.id.content, checkout, caas, money, cardAuthWindow,
                newFlow, useVCN());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
        outState.putString(CHECKOUT_CAAS_EXTRA, caas);
        outState.putSerializable(CHECKOUT_MONEY, money);
        outState.putBoolean(NEW_FLOW, newFlow);
        outState.putInt(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
    }
}

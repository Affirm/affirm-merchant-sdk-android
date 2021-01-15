package com.affirm.android;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

import org.joda.money.Money;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.NEW_FLOW;

abstract class CheckoutBaseActivity extends AffirmActivity {

    private CheckoutRequest checkoutRequest;

    protected Checkout checkout;

    private Money money;

    protected boolean newFlow;

    protected String caas;

    private int cardAuthWindow;

    abstract boolean useVCN();

    abstract InnerCheckoutCallback getInnerCheckoutCallback();

    @Override
    void beforeOnCreate() {
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
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
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
        outState.putString(CHECKOUT_CAAS_EXTRA, caas);
        outState.putSerializable(CHECKOUT_MONEY, money);
        outState.putBoolean(NEW_FLOW, newFlow);
    }

    @Override
    void onAttached() {
        checkoutRequest = new CheckoutRequest(checkout, getInnerCheckoutCallback(), caas, money,
                useVCN(), cardAuthWindow);
        checkoutRequest.create();
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

package com.affirm.android;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.Checkout;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.CHECKOUT_SESSION_CODE;
import static com.affirm.android.AffirmConstants.CHECKOUT_SESSION_CODE_USED;

import org.joda.money.Money;

abstract class CheckoutBaseFragment extends AffirmFragment {

    private CheckoutRequest checkoutRequest;

    private Checkout checkout;

    private Money money;

    private String caas;

    private int cardAuthWindow;

    private String sessionCode;

    // A session code is single use, so only the first checkout load may carry it. Reloads after
    // rotation or restore must not pair it with a verifier from a later prepareAuth.
    private boolean sessionCodeUsed;

    abstract InnerCheckoutCallback innerCheckoutCallback();

    abstract boolean useVCN();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkout = getArguments().getParcelable(CHECKOUT_EXTRA);
        caas = getArguments().getString(CHECKOUT_CAAS_EXTRA);
        money = (Money) getArguments().getSerializable(CHECKOUT_MONEY);
        cardAuthWindow = getArguments().getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
        sessionCode = getArguments().getString(CHECKOUT_SESSION_CODE);
        if (savedInstanceState != null) {
            sessionCodeUsed = savedInstanceState.getBoolean(CHECKOUT_SESSION_CODE_USED);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHECKOUT_SESSION_CODE_USED, sessionCodeUsed);
    }

    @Override
    void onAttached() {
        checkoutRequest = new CheckoutRequest(checkout, innerCheckoutCallback(), caas, money,
                useVCN(), cardAuthWindow);
        checkoutRequest.create();
    }

    @NonNull
    String connectedAccountsRedirectUrl(@NonNull String redirectUrl) {
        if (sessionCode == null) {
            return redirectUrl;
        }
        final String codeVerifier =
                sessionCodeUsed ? null : ConnectedAccounts.consumeCodeVerifier();
        sessionCodeUsed = true;
        return ConnectedAccounts.decorateRedirectUrl(redirectUrl, sessionCode, codeVerifier);
    }

    @Override
    public void onDestroy() {
        checkoutRequest.cancel();
        super.onDestroy();
    }
}
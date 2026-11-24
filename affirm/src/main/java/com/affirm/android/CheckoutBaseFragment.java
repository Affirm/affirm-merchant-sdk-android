package com.affirm.android;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.model.Checkout;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.CHECKOUT_SESSION_CODE;
import static com.affirm.android.AffirmConstants.CHECKOUT_SESSION_CODE_CONSUMED;

import org.joda.money.Money;

abstract class CheckoutBaseFragment extends AffirmFragment {

    @VisibleForTesting
    CheckoutRequest checkoutRequest;

    private Checkout checkout;

    private Money money;

    private String caas;

    private int cardAuthWindow;

    private String sessionCode;

    // Set on the first checkout load even if decoration fails: a session code is single use.
    private boolean sessionCodeConsumed;

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
            sessionCodeConsumed = savedInstanceState.getBoolean(CHECKOUT_SESSION_CODE_CONSUMED);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CHECKOUT_SESSION_CODE_CONSUMED, sessionCodeConsumed);
    }

    @Override
    void onAttached() {
        // The fragment is retained, so this runs again after rotation while the previous
        // request may still be in flight; a late response would replace the newer page.
        if (checkoutRequest != null) {
            checkoutRequest.cancel();
        }
        checkoutRequest = new CheckoutRequest(checkout, innerCheckoutCallback(), caas, money,
                useVCN(), cardAuthWindow);
        checkoutRequest.create();
    }

    @NonNull
    String connectedAccountsRedirectUrl(@NonNull String redirectUrl) {
        if (TextUtils.isEmpty(sessionCode)) {
            return redirectUrl;
        }
        if (sessionCodeConsumed) {
            ConnectedAccounts.trackDecoration(ConnectedAccounts.DecorateReason.ALREADY_CONSUMED,
                    redirectUrl);
            return redirectUrl;
        }
        sessionCodeConsumed = true;
        return ConnectedAccounts.decorateRedirectUrl(redirectUrl, sessionCode,
                ConnectedAccounts.consumeCodeVerifier());
    }

    @Override
    public void onDestroy() {
        checkoutRequest.cancel();
        super.onDestroy();
    }
}
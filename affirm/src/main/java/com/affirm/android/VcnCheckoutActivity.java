package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.model.CardDetails;
import com.affirm.android.model.VcnReason;
import com.affirm.android.model.Checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

public class VcnCheckoutActivity extends CheckoutBaseActivity
        implements Affirm.VcnCheckoutCallbacks {

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout, @Nullable String caas,
                              int cardAuthWindow) {
        Intent intent = buildIntent(activity, checkout, caas, cardAuthWindow);
        startForResult(activity, intent, requestCode);
    }

    static void startActivity(@NonNull Fragment fragment, int requestCode,
                              @NonNull Checkout checkout, @Nullable String caas,
                              int cardAuthWindow) {
        Intent intent = buildIntent(fragment.requireActivity(), checkout, caas, cardAuthWindow);
        startForResult(fragment, intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            @NonNull Checkout checkout, @Nullable String caas,
            int cardAuthWindow) {
        final Intent intent = new Intent(originalActivity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        intent.putExtra(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
        return intent;
    }

    @Override
    boolean useVCN() {
        return true;
    }

    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        finishWithError(message);
    }

    @Override
    public void onAffirmVcnCheckoutCancelled() {
        finishWithCancellation();
    }

    @Override
    public void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason) {
        finishWithVcnReason(vcnReason);
    }

    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        finishWithCardDetail(cardDetails);
    }
}

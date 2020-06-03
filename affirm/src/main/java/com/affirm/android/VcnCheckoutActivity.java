package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.VcnReason;

import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

public class VcnCheckoutActivity extends AffirmActivity implements Affirm.VcnCheckoutCallbacks {

    private Checkout checkout;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Hide the actionbar because the Affirm screen is already included
        AffirmUtils.hideActionBar(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }
        Affirm.startCheckout(this, android.R.id.content, checkout, true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CHECKOUT_EXTRA, checkout);
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

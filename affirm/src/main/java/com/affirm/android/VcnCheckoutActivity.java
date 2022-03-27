package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.affirm.android.model.CardDetails;
import com.affirm.android.model.CardDetailsInner;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.VcnReason;

import org.joda.money.Money;

import java.util.Calendar;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.NEW_FLOW;

public class VcnCheckoutActivity extends CheckoutBaseActivity
        implements Affirm.VcnCheckoutCallbacks {

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout, @Nullable String caas,
                              @Nullable Money money, int cardAuthWindow,
                              boolean newFlow) {
        Intent intent = buildIntent(activity, checkout, caas, money, cardAuthWindow, newFlow);
        startForResult(activity, intent, requestCode);
    }

    static void startActivity(@NonNull Fragment fragment, int requestCode,
                              @NonNull Checkout checkout, @Nullable String caas,
                              @Nullable Money money, int cardAuthWindow, boolean newFlow) {
        Intent intent = buildIntent(fragment.requireActivity(), checkout, caas, money,
                cardAuthWindow, newFlow);
        startForResult(fragment, intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            @NonNull Checkout checkout, @Nullable String caas, @Nullable Money money,
            int cardAuthWindow, boolean newFlow) {

        final Intent intent = new Intent(originalActivity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        intent.putExtra(CHECKOUT_MONEY, money);
        intent.putExtra(NEW_FLOW, newFlow);
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
        if (newFlow) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 24);
            AffirmPlugins.get().setCacheCardDetails(
                    new CardDetailsInner(cardDetails, calendar.getTime()));
            Affirm.startVcnDisplay(this, checkout, caas);
        } else {
            finishWithCardDetail(cardDetails);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}

package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.math.BigDecimal;

import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_ID;

public class PrequalActivity extends AffirmActivity implements Affirm.PrequalCallbacks {

    private BigDecimal amount;
    private String promoId;
    private String pageType;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              BigDecimal amount, @Nullable String promoId,
                              @Nullable String pageType) {
        Intent intent = buildIntent(activity, amount, promoId, pageType);
        startForResult(activity, intent, requestCode);
    }

    static void startActivity(@NonNull Fragment fragment, int requestCode,
                              BigDecimal amount, @Nullable String promoId,
                              @Nullable String pageType) {
        Intent intent = buildIntent(fragment.requireActivity(), amount, promoId, pageType);
        startForResult(fragment, intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            BigDecimal amount,
            @Nullable String promoId,
            @Nullable String pageType) {
        final Intent intent = new Intent(originalActivity, PrequalActivity.class);
        intent.putExtra(AMOUNT, amount);
        intent.putExtra(PROMO_ID, promoId);
        intent.putExtra(PAGE_TYPE, pageType);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            amount = (BigDecimal) savedInstanceState.getSerializable(AMOUNT);
            promoId = savedInstanceState.getString(PROMO_ID);
            pageType = savedInstanceState.getString(PAGE_TYPE);
        } else {
            amount = (BigDecimal) getIntent().getSerializableExtra(AMOUNT);
            promoId = getIntent().getStringExtra(PROMO_ID);
            pageType = getIntent().getStringExtra(PAGE_TYPE);
        }
        PrequalFragment.newInstance(this, android.R.id.content, amount, promoId, pageType);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(AMOUNT, amount);
        outState.putString(PROMO_ID, promoId);
        outState.putString(PAGE_TYPE, pageType);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        finishWithPrequalError(message);
    }
}

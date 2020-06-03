package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;

import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.MODAL_ID;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.TYPE_EXTRA;

public class ModalActivity extends AffirmActivity implements Affirm.PrequalCallbacks {

    private ModalFragment.ModalType type;
    private BigDecimal amount;
    private String modalId;
    private String pageType;
    private String promoId;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull BigDecimal amount, @NonNull ModalFragment.ModalType type,
                              @Nullable String modalId, @Nullable String pageType,
                              @Nullable String promoId) {
        final Intent intent = new Intent(activity, ModalActivity.class);
        intent.putExtra(AMOUNT, amount);
        intent.putExtra(TYPE_EXTRA, type);
        intent.putExtra(MODAL_ID, modalId == null ? "" : modalId);
        intent.putExtra(PAGE_TYPE, pageType == null ? "" : pageType);
        intent.putExtra(PROMO_ID, promoId == null ? "" : promoId);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Hide the actionbar because the Affirm screen is already included
        AffirmUtils.hideActionBar(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            amount = (BigDecimal) savedInstanceState.getSerializable(AMOUNT);
            type = (ModalFragment.ModalType) savedInstanceState.getSerializable(TYPE_EXTRA);
            modalId = savedInstanceState.getString(MODAL_ID);
            pageType = savedInstanceState.getString(PAGE_TYPE);
            promoId = savedInstanceState.getString(PROMO_ID);
        } else {
            amount = (BigDecimal) getIntent().getSerializableExtra(AMOUNT);
            type = (ModalFragment.ModalType) getIntent().getSerializableExtra(TYPE_EXTRA);
            modalId = getIntent().getStringExtra(MODAL_ID);
            pageType = getIntent().getStringExtra(PAGE_TYPE);
            promoId = getIntent().getStringExtra(PROMO_ID);
        }
        ModalFragment.newInstance(this, android.R.id.content, amount, type, modalId,
                pageType, promoId);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(AMOUNT, amount);
        outState.putSerializable(TYPE_EXTRA, type);
        outState.putString(MODAL_ID, modalId == null ? "" : modalId);
        outState.putString(PAGE_TYPE, pageType == null ? "" : pageType);
        outState.putString(PROMO_ID, promoId == null ? "" : promoId);
    }

    @Override
    public void onAffirmPrequalError(@Nullable String message) {
        finishWithPrequalError(message);
    }
}



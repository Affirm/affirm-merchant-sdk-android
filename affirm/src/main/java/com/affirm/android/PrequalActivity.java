package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.Item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.ITEMS;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_ID;

public class PrequalActivity extends AffirmActivity implements Affirm.PrequalCallbacks {

    private BigDecimal amount;
    private String promoId;
    private String pageType;
    private List<Item> items;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              BigDecimal amount, @Nullable String promoId,
                              @Nullable String pageType, @Nullable List<Item> items) {
        final Intent intent = new Intent(activity, PrequalActivity.class);
        intent.putExtra(AMOUNT, amount);
        intent.putExtra(PROMO_ID, promoId);
        intent.putExtra(PAGE_TYPE, pageType);
        if (items != null) {
            intent.putParcelableArrayListExtra(ITEMS, new ArrayList<>(items));
        }
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AffirmUtils.showCloseActionBar(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            amount = (BigDecimal) savedInstanceState.getSerializable(AMOUNT);
            promoId = savedInstanceState.getString(PROMO_ID);
            pageType = savedInstanceState.getString(PAGE_TYPE);
            items = savedInstanceState.getParcelableArrayList(ITEMS);
        } else {
            amount = (BigDecimal) getIntent().getSerializableExtra(AMOUNT);
            promoId = getIntent().getStringExtra(PROMO_ID);
            pageType = getIntent().getStringExtra(PAGE_TYPE);
            items = getIntent().getParcelableArrayListExtra(ITEMS);
        }
        PrequalFragment.newInstance(this, android.R.id.content, amount, promoId, pageType, items);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(AMOUNT, amount);
        outState.putString(PROMO_ID, promoId);
        outState.putString(PAGE_TYPE, pageType);
        outState.putParcelableArrayList(ITEMS, new ArrayList<>(items));
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

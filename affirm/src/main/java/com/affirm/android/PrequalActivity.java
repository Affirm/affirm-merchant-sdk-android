package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.Item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.ITEMS;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_ERROR;
import static com.affirm.android.AffirmConstants.PREQUAL_PATH;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.REFERRING_URL;
import static com.affirm.android.AffirmTracker.TrackingEvent.PREQUAL_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;

public class PrequalActivity extends AffirmActivity implements PrequalWebViewClient.Callbacks {

    private String amount;
    private String promoId;
    private String pageType;
    private List<Item> items;

    static void startActivity(@NonNull Activity originalActivity, @Nullable Fragment fragment,
                              int requestCode, BigDecimal amount, @Nullable String promoId,
                              @Nullable String pageType, @Nullable List<Item> items) {
        final Intent intent = new Intent(originalActivity, PrequalActivity.class);
        final String stringAmount =
                String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        intent.putExtra(AMOUNT, stringAmount);
        intent.putExtra(PROMO_ID, promoId);
        intent.putExtra(PAGE_TYPE, pageType);
        if (items != null) {
            intent.putParcelableArrayListExtra(ITEMS, new ArrayList<>(items));
        }
        startForResult(originalActivity, fragment, intent, requestCode);
    }

    @Override
    void beforeOnCreate() {
        AffirmUtils.showCloseActionBar(this);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new PrequalWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            amount = savedInstanceState.getString(AMOUNT);
            promoId = savedInstanceState.getString(PROMO_ID);
            pageType = savedInstanceState.getString(PAGE_TYPE);
            items = savedInstanceState.getParcelableArrayList(ITEMS);
        } else {
            amount = getIntent().getStringExtra(AMOUNT);
            promoId = getIntent().getStringExtra(PROMO_ID);
            pageType = getIntent().getStringExtra(PAGE_TYPE);
            items = getIntent().getParcelableArrayListExtra(ITEMS);
        }
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        StringBuilder path = new StringBuilder(
                String.format(PREQUAL_PATH, publicKey, amount, promoId, REFERRING_URL)
        );
        if (pageType != null) {
            path.append("&page_type=").append(pageType);
        }
        if (items != null) {
            path.append("&items=").append(Uri.encode(AffirmPlugins.get().gson().toJson(items)));
        }
        webView.loadUrl(HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl() + path.toString());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(AMOUNT, amount);
        outState.putString(PROMO_ID, promoId);
        outState.putString(PAGE_TYPE, pageType);
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(PREQUAL_WEBVIEW_FAIL, ERROR, null);
        final Intent intent = new Intent();
        intent.putExtra(PREQUAL_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
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
    public void onWebViewConfirmation() {
        finish();
    }
}

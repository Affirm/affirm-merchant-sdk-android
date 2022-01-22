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

import java.math.BigDecimal;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_ERROR;
import static com.affirm.android.AffirmConstants.PREQUAL_IS_SDK;
import static com.affirm.android.AffirmConstants.PREQUAL_PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_PATH;
import static com.affirm.android.AffirmConstants.PREQUAL_PROMO_EXTERNAL_ID;
import static com.affirm.android.AffirmConstants.PREQUAL_PUBLIC_API_KEY;
import static com.affirm.android.AffirmConstants.PREQUAL_REFERRING_URL;
import static com.affirm.android.AffirmConstants.PREQUAL_UNIT_PRICE;
import static com.affirm.android.AffirmConstants.PREQUAL_USE_PROMO;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.REFERRING_URL;
import static com.affirm.android.AffirmTracker.TrackingEvent.PREQUAL_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;

public class PrequalActivity extends AffirmActivity implements PrequalWebViewClient.Callbacks {

    private String amount;
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
        final String stringAmount =
                String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        intent.putExtra(AMOUNT, stringAmount);
        intent.putExtra(PROMO_ID, promoId);
        intent.putExtra(PAGE_TYPE, pageType);
        return intent;
    }

    @Override
    void beforeOnCreate() {
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
        } else {
            amount = getIntent().getStringExtra(AMOUNT);
            promoId = getIntent().getStringExtra(PROMO_ID);
            pageType = getIntent().getStringExtra(PAGE_TYPE);
        }
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        String prequalUri = HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl() + PREQUAL_PATH;
        Uri.Builder builder = Uri.parse(prequalUri).buildUpon();
        builder.appendQueryParameter(PREQUAL_PUBLIC_API_KEY, publicKey);
        builder.appendQueryParameter(PREQUAL_UNIT_PRICE, amount);
        builder.appendQueryParameter(PREQUAL_USE_PROMO, "true");
        builder.appendQueryParameter(PREQUAL_IS_SDK, "true");
        builder.appendQueryParameter(PREQUAL_REFERRING_URL, REFERRING_URL);
        if (promoId != null) {
            builder.appendQueryParameter(PREQUAL_PROMO_EXTERNAL_ID, promoId);
        }
        if (pageType != null) {
            builder.appendQueryParameter(PREQUAL_PAGE_TYPE, pageType);
        }
        webView.loadUrl(builder.build().toString());
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

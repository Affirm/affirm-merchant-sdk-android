package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.affirm.android.exception.ConnectionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PrequalActivity extends AffirmActivity
        implements AffirmWebViewClient.Callbacks, PrequalWebViewClient.Callbacks {

    private static final String AMOUNT = "AMOUNT";
    private static final String PROMO_ID = "PROMO_ID";

    private String amount;
    private String promoId;

    static void startActivity(@NonNull Context context, float amount, @Nullable String promoId) {
        final Intent intent = new Intent(context, PrequalActivity.class);
        intent.putExtra(AMOUNT, String.valueOf(amount));
        intent.putExtra(PROMO_ID, promoId);
        context.startActivity(intent);
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
        } else {
            amount = getIntent().getStringExtra(AMOUNT);
            promoId = getIntent().getStringExtra(PROMO_ID);
        }
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        String path = String.format(
                "/apps/prequal?public_api_key=%s"
                        + "&unit_price=%s"
                        + "&promo_external_id=%s"
                        + "&isSDK=true"
                        + "&use_promo=True"
                        + "&referring_url=%s",
                publicKey, amount, promoId, PrequalWebViewClient.REFERRING_URL);

        webView.loadUrl(PROTOCOL + AffirmPlugins.get().baseUrl() + path);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(AMOUNT, amount);
        outState.putString(PROMO_ID, promoId);
    }

    @Override
    public void onWebViewCancellation() {
        finish();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onWebViewConfirmation() {
        finish();
    }
}

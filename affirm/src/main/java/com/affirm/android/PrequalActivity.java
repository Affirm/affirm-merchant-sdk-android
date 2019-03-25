package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.affirm.android.exception.ConnectionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmTracker.TrackingEvent.PREQUAL_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.Constants.AMOUNT;
import static com.affirm.android.Constants.HTTPS_PROTOCOL;
import static com.affirm.android.Constants.PREQUAL_ERROR;
import static com.affirm.android.Constants.PREQUAL_PATH;
import static com.affirm.android.Constants.PROMO_ID;
import static com.affirm.android.Constants.REFERRING_URL;

public class PrequalActivity extends AffirmActivity
        implements AffirmWebViewClient.Callbacks, PrequalWebViewClient.Callbacks {

    private String mAmount;
    private String mPromoId;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              float amount, @Nullable String promoId) {
        final Intent intent = new Intent(activity, PrequalActivity.class);
        intent.putExtra(AMOUNT, String.valueOf(amount));
        intent.putExtra(PROMO_ID, promoId);
        activity.startActivityForResult(intent, requestCode);
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
            mAmount = savedInstanceState.getString(AMOUNT);
            mPromoId = savedInstanceState.getString(PROMO_ID);
        } else {
            mAmount = getIntent().getStringExtra(AMOUNT);
            mPromoId = getIntent().getStringExtra(PROMO_ID);
        }
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        String path = String.format(PREQUAL_PATH,
                publicKey, mAmount, mPromoId, REFERRING_URL);

        webView.loadUrl(HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl() + path);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(AMOUNT, mAmount);
        outState.putString(PROMO_ID, mPromoId);
    }

    @Override
    public void onWebViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
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

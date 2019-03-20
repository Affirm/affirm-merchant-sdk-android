package com.affirm.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import com.affirm.android.exception.ConnectionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.affirm.android.Constants.AMOUNT;
import static com.affirm.android.Constants.HTTPS_PROTOCOL;
import static com.affirm.android.Constants.PREQUAL_PATH;
import static com.affirm.android.Constants.PROMO_ID;
import static com.affirm.android.Constants.REFERRING_URL;

public class PrequalActivity extends AffirmActivity
        implements AffirmWebViewClient.Callbacks, PrequalWebViewClient.Callbacks {

    private String mAmount;
    private String mPromoId;

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
                actionHome();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void actionHome() {
        String defaultName;
        if (AffirmPlugins.get().environment() == Affirm.Environment.SANDBOX) {
            defaultName = getString(R.string.affirm_name_default_sandbox);
        } else {
            defaultName = getString(R.string.affirm_name_default_production);
        }
        String name = TextUtils.isEmpty(AffirmPlugins.get().name()) ?
                defaultName : AffirmPlugins.get().name();
        new AlertDialog.Builder(this).setTitle(R.string.affirm)
                .setMessage(getString(R.string.affirm_prequal_quit_confirm, name))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onWebViewConfirmation() {
        finish();
    }
}

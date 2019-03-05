package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import com.affirm.android.http.AffirmHttpBody;
import com.affirm.android.http.AffirmHttpRequest;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;

import androidx.annotation.NonNull;

public class CheckoutActivity extends CheckoutBaseActivity implements CheckoutWebViewClient.Callbacks {

    public static final String CHECKOUT_TOKEN = "checkout_token";

    static void startActivity(@NonNull Activity activity, int requestCode, @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, CheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void startCheckout() {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();

        final Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setConfirmationUrl(AffirmWebViewClient.AFFIRM_CONFIRMATION_URL)
                .setCancelUrl(AffirmWebViewClient.AFFIRM_CANCELLATION_URL)
                .setName(AffirmPlugins.get().name())
                .build();

        final JsonObject jsonRequest = buildJsonRequest(checkout, merchant);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl("https://sandbox.affirm.com/api/v2/checkout/")
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody("application/json; charset=utf-8", jsonRequest.toString()))
                .build();

        httpClient.execute(request, CheckoutResponse.class, new AffirmHttpClient.Callback<CheckoutResponse>() {
            @Override
            public void onSuccess(final CheckoutResponse response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(response.redirectUrl());
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                onWebViewError(throwable);
            }
        });
    }

    @Override
    void setupWebView() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new CheckoutWebViewClient(this));
        webView.setWebChromeClient(new PopUpWebChromeClient(this));
    }

    @Override
    public void onWebViewConfirmation(@NonNull String token) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_TOKEN, token);
        setResult(RESULT_OK, intent);
        finish();
    }
}

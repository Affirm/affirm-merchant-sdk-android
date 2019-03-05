package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.affirm.android.http.AffirmHttpBody;
import com.affirm.android.http.AffirmHttpRequest;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

public class VcnCheckoutActivity extends CheckoutBaseActivity implements PopUpWebChromeClient.Callbacks, VcnCheckoutWebViewClient.Callbacks {

    public static final String CREDIT_DETAILS = "credit_details";

    static void startActivity(@NonNull Activity activity, int requestCode, @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void startCheckout() {
        AffirmHttpClient httpClient = AffirmPlugins.get().restClient();

        final Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setUseVcn(true)
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

                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadDataWithBaseURL("https://" + uri.getHost(), html, "text/html", "utf-8", null);
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
        webView.setWebViewClient(
                new VcnCheckoutWebViewClient(AffirmPlugins.get().gson(), this));
        webView.setWebChromeClient(new PopUpWebChromeClient(this));
        clearCookies();
    }

    private String initialHtml(CheckoutResponse response) {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(R.raw.vcn_checkout);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final HashMap<String, String> map = new HashMap<>();

        map.put("URL", response.redirectUrl());
        map.put("URL2", response.redirectUrl());
        map.put("JS_CALLBACK_ID", response.jsCallbackId());
        map.put("CONFIRM_CB_URL", AffirmWebViewClient.AFFIRM_CONFIRMATION_URL);
        map.put("CANCELLED_CB_URL", AffirmWebViewClient.AFFIRM_CANCELLATION_URL);
        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onWebViewConfirmation(@NonNull CardDetails cardDetails) {
        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(RESULT_OK, intent);
        finish();
    }
}

package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CANCELLED_CB_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CONFIRM_CB_URL;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.URL;
import static com.affirm.android.AffirmConstants.URL2;
import static com.affirm.android.AffirmConstants.UTF_8;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

public class VcnCheckoutActivity extends CheckoutBaseActivity
        implements VcnCheckoutWebViewClient.Callbacks {

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(
                new VcnCheckoutWebViewClient(AffirmPlugins.get().gson(), this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
        clearCookies();
    }

    @Override
    boolean useVCN() {
        return true;
    }

    @Override
    InnerCheckoutCallback getInnerCheckoutCallback() {
        return new InnerCheckoutCallback() {
            @Override
            public void onError(@NonNull AffirmException exception) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_FAIL, ERROR, null);
                finishWithError(exception);
            }

            @Override
            public void onSuccess(@NonNull CheckoutResponse response) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_SUCCESS, INFO, null);
                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());
                webView.loadDataWithBaseURL(HTTPS_PROTOCOL + uri.getHost(), html,
                        TEXT_HTML, UTF_8, null);
            }
        };
    }

    private String initialHtml(@NonNull CheckoutResponse response) {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(R.raw.affirm_vcn_checkout);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final HashMap<String, String> map = new HashMap<>();

        map.put(URL, response.redirectUrl());
        map.put(URL2, response.redirectUrl());
        map.put(CONFIRM_CB_URL, AFFIRM_CHECKOUT_CONFIRMATION_URL);
        map.put(CANCELLED_CB_URL, AFFIRM_CHECKOUT_CANCELLATION_URL);
        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onWebViewConfirmation(@NonNull CardDetails cardDetails) {
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_SUCCESS, INFO, null);

        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_FAIL, ERROR, null);

        finishWithError(error);
    }

    @Override
    public void onWebViewCancellation() {
        webViewCancellation();
    }
}

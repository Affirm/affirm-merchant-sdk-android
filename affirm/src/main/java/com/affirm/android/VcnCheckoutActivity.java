package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

public class VcnCheckoutActivity extends CheckoutCommonActivity
        implements AffirmWebChromeClient.Callbacks, VcnCheckoutWebViewClient.Callbacks {

    public static final String CREDIT_DETAILS = "credit_details";

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void beforeOnCreate() {
        super.beforeOnCreate();
        checkoutType = CheckoutRequest.CheckoutType.VCN;
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
    void onAttached() {
        InnerCheckoutCallback checkoutCallback = new InnerCheckoutCallback() {
            @Override
            public void onError(@NonNull AffirmException exception) {
                AffirmTracker.get().track(VCN_CHECKOUT_CREATION_FAIL, ERROR, null);
                finishWithError(exception);
            }

            @Override
            public void onSuccess(@NonNull CheckoutResponse response) {
                AffirmTracker.get().track(VCN_CHECKOUT_CREATION_SUCCESS, INFO, null);
                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());
                webView.loadDataWithBaseURL("https://" + uri.getHost(), html,
                        "text/html", "utf-8", null);
            }
        };

        checkoutRequest = new CheckoutRequest(this, checkout,
                checkoutCallback, CheckoutRequest.CheckoutType.VCN);
        checkoutRequest.create();
    }

    @Override
    CheckoutResponse executeTask(@NonNull Checkout checkout) throws APIException,
            PermissionException, InvalidRequestException, ConnectionException {
        return AffirmApiHandler.executeVcnCheckout(checkout);
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
        AffirmTracker.get().track(VCN_CHECKOUT_WEBVIEW_SUCCESS, INFO, null);

        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.get().track(VCN_CHECKOUT_WEBVIEW_FAIL, ERROR, null);

        finishWithError(error);
    }

    @Override
    public void onWebViewCancellation() {
        webViewCancellation();
    }
}

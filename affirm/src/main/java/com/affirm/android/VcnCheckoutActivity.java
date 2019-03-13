package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.affirm.android.exception.APIException;
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

class VcnCheckoutActivity extends CheckoutBaseActivity implements AffirmWebChromeClient.Callbacks
    , VcnCheckoutWebViewClient.Callbacks {

    public static final String CREDIT_DETAILS = "credit_details";

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
    void onAttached() {
        CheckoutCallback checkoutCallback = new CheckoutCallback() {
            @Override
            public void onError(Exception exception) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_FAIL, ERROR, null);
                finishWithError(exception);
            }

            @Override
            public void onSuccess(CheckoutResponse response) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_SUCCESS, INFO, null);
                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());
                webView.loadDataWithBaseURL("https://" + uri.getHost(), html, "text/html", "utf-8"
                    , null);
            }
        };

        taskCreator.create(this, checkout, checkoutCallback);
    }

    @Override
    CheckoutResponse executeTask(Checkout checkout) throws IOException, APIException, PermissionException, InvalidRequestException {
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
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_SUCCESS, INFO, null);

        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_FAIL, ERROR, null);

        finishWithError(error);
    }

    @Override
    public void onWebViewCancellation() {
        super.onWebViewCancellation();
    }

    @Override
    protected void onDestroy() {
        AffirmApiHandler.cancelVcnCheckoutCall();
        super.onDestroy();
    }
}

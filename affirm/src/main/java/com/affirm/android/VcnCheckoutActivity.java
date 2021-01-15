package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.CardDetailsInner;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.VcnReason;

import org.joda.money.Money;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CANCELLED_CB_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.CONFIRM_CB_URL;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.NEW_FLOW;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.URL;
import static com.affirm.android.AffirmConstants.URL2;
import static com.affirm.android.AffirmConstants.UTF_8;
import static com.affirm.android.AffirmConstants.VCN_REASON;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

public class VcnCheckoutActivity extends CheckoutBaseActivity
        implements VcnCheckoutWebViewClient.Callbacks {

    private static String receiveReasonCodes;

    static void startActivity(@NonNull Activity activity, int requestCode,
                              @NonNull Checkout checkout, @Nullable String caas,
                              @Nullable Money money, int cardAuthWindow,
                              @NonNull String configReceiveReasonCodes,
                              boolean newFlow) {


        receiveReasonCodes = configReceiveReasonCodes;
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        intent.putExtra(CHECKOUT_MONEY, money);
        intent.putExtra(NEW_FLOW, newFlow);
        intent.putExtra(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(
                new VcnCheckoutWebViewClient(AffirmPlugins.get().gson(), receiveReasonCodes, this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
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
        if (newFlow) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 24);
            AffirmPlugins.get().setCacheCardDetails(
                    new CardDetailsInner(cardDetails, calendar.getTime()));
            Affirm.startVcnDisplay(this, checkout, caas);
        } else {
            final Intent intent = new Intent();
            intent.putExtra(CREDIT_DETAILS, cardDetails);
            setResult(RESULT_OK, intent);
            finish();
        }
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

    @Override
    public void onWebViewCancellationReason(@NonNull VcnReason vcnReason) {
        final Intent intent = new Intent();
        intent.putExtra(VCN_REASON, vcnReason);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}

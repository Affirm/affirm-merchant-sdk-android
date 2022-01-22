package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.affirm.android.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.fragment.app.Fragment;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.CANCEL_URL;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.MAP_EXTRA;
import static com.affirm.android.AffirmConstants.MODAL_ID;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_ERROR;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.TYPE_EXTRA;
import static com.affirm.android.AffirmConstants.UTF_8;
import static com.affirm.android.AffirmTracker.TrackingEvent.PRODUCT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.SITE_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;

public class ModalActivity extends AffirmActivity implements ModalWebViewClient.Callbacks {

    private ModalType type;
    private HashMap<String, String> map;

    enum ModalType {
        PRODUCT(R.raw.affirm_modal_template, PRODUCT_WEBVIEW_FAIL),
        SITE(R.raw.affirm_modal_template, SITE_WEBVIEW_FAIL);

        @RawRes
        final int templateRes;
        final AffirmTracker.TrackingEvent failureEvent;

        ModalType(int templateRes, AffirmTracker.TrackingEvent failureEvent) {
            this.templateRes = templateRes;
            this.failureEvent = failureEvent;
        }
    }

    static void startActivity(@NonNull Activity activity, int requestCode, BigDecimal amount,
                              ModalType type, @Nullable String modalId, @Nullable String pageType,
                              @Nullable String promoId) {
        Intent intent = buildIntent(activity, amount, type, modalId, pageType, promoId);
        startForResult(activity, intent, requestCode);
    }


    static void startActivity(@NonNull Fragment fragment, int requestCode, BigDecimal amount,
                              ModalType type, @Nullable String modalId, @Nullable String pageType,
                              @Nullable String promoId) {
        Intent intent = buildIntent(fragment.requireActivity(), amount, type, modalId,
                pageType, promoId);
        startForResult(fragment, intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            BigDecimal amount,
            ModalType type,
            @Nullable String modalId,
            @Nullable String pageType,
            @Nullable String promoId) {
        final Intent intent = new Intent(originalActivity, ModalActivity.class);
        final String stringAmount =
                String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseJsUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();
        map.put(AMOUNT, stringAmount);
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(CANCEL_URL, AFFIRM_CHECKOUT_CANCELLATION_URL);
        map.put(MODAL_ID, modalId == null ? "" : modalId);
        map.put(PAGE_TYPE, pageType == null ? "" : pageType);
        map.put(PROMO_ID, promoId == null ? "" : promoId);

        intent.putExtra(TYPE_EXTRA, type);
        intent.putExtra(MAP_EXTRA, map);
        return intent;
    }

    @Override
    void beforeOnCreate() {
        AffirmUtils.hideActionBar(this);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new ModalWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            map = (HashMap<String, String>) savedInstanceState.getSerializable(MAP_EXTRA);
            type = (ModalType) savedInstanceState.getSerializable(TYPE_EXTRA);
        } else {
            map = (HashMap<String, String>) getIntent().getSerializableExtra(MAP_EXTRA);
            type = (ModalType) getIntent().getSerializableExtra(TYPE_EXTRA);
        }
    }

    @Override
    void onAttached() {
        final String html = initialHtml();
        webView.loadDataWithBaseURL(
                HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl(),
                html, TEXT_HTML, UTF_8, null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(TYPE_EXTRA, type.templateRes);
        outState.putSerializable(MAP_EXTRA, map);
    }

    private String initialHtml() {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(type.templateRes);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onWebViewCancellation() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(type.failureEvent, ERROR, null);
        finish();

        final Intent intent = new Intent();
        intent.putExtra(PREQUAL_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
    }
}



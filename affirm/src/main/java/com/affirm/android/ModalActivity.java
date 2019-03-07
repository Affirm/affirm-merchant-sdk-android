package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import static com.affirm.android.AffirmTracker.TrackingEvent.PRODUCT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.SITE_WEBVIEW_FAIL;

class ModalActivity extends AffirmActivity
        implements AffirmWebViewClient.Callbacks {

    private static final String MAP_EXTRA = "MAP_EXTRA";
    private static final String TYPE_EXTRA = "TYPE_EXTRA";

    private static final String JS_PATH = "/js/v2/affirm.js";

    private static final String AMOUNT = "AMOUNT";
    private static final String API_KEY = "API_KEY";
    private static final String JAVASCRIPT = "JAVASCRIPT";
    private static final String CANCEL_URL = "CANCEL_URL";
    private static final String MODAL_ID = "MODAL_ID";

    private ModalType type;
    private HashMap<String, String> map;

    enum ModalType {
        PRODUCT(R.raw.modal_template, PRODUCT_WEBVIEW_FAIL),
        SITE(R.raw.modal_template, SITE_WEBVIEW_FAIL);

        @RawRes
        final int templateRes;
        final AffirmTracker.TrackingEvent failureEvent;

        ModalType(int templateRes, AffirmTracker.TrackingEvent failureEvent) {
            this.templateRes = templateRes;
            this.failureEvent = failureEvent;
        }
    }

    static void startActivity(@NonNull Context context, float amount, ModalType type, @Nullable String modalId) {
        final Intent intent = new Intent(context, ModalActivity.class);
        final String stringAmount = String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        final String fullPath = PROTOCOL + AffirmPlugins.get().baseUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();
        map.put(AMOUNT, stringAmount);
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(CANCEL_URL, AffirmWebViewClient.AFFIRM_CANCELLATION_URL);
        map.put(MODAL_ID, modalId == null ? "" : modalId);

        intent.putExtra(TYPE_EXTRA, type);
        intent.putExtra(MAP_EXTRA, map);

        context.startActivity(intent);
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
        webView.loadDataWithBaseURL(PROTOCOL + AffirmPlugins.get().baseUrl(), html, "text/html", "utf-8", null);
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
        finish();
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        finish();
    }
}



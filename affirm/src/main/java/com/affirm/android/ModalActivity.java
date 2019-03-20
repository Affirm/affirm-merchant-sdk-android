package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.affirm.android.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import static com.affirm.android.AffirmTracker.TrackingEvent.PRODUCT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.SITE_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.Constants.AFFIRM_CANCELLATION_URL;
import static com.affirm.android.Constants.AMOUNT;
import static com.affirm.android.Constants.API_KEY;
import static com.affirm.android.Constants.CANCEL_URL;
import static com.affirm.android.Constants.HTTPS_PROTOCOL;
import static com.affirm.android.Constants.JAVASCRIPT;
import static com.affirm.android.Constants.JS_PATH;
import static com.affirm.android.Constants.MAP_EXTRA;
import static com.affirm.android.Constants.MODAL_ID;
import static com.affirm.android.Constants.TYPE_EXTRA;

public class ModalActivity extends AffirmActivity
    implements AffirmWebViewClient.Callbacks {

    private ModalType mType;
    private HashMap<String, String> mMap;

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

    static void startActivity(@NonNull Context context, float amount, ModalType type,
                              @Nullable String modalId) {
        final Intent intent = new Intent(context, ModalActivity.class);
        final String stringAmount =
            String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();
        map.put(AMOUNT, stringAmount);
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(CANCEL_URL, AFFIRM_CANCELLATION_URL);
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
            mMap = (HashMap<String, String>) savedInstanceState.getSerializable(MAP_EXTRA);
            mType = (ModalType) savedInstanceState.getSerializable(TYPE_EXTRA);
        } else {
            mMap = (HashMap<String, String>) getIntent().getSerializableExtra(MAP_EXTRA);
            mType = (ModalType) getIntent().getSerializableExtra(TYPE_EXTRA);
        }
    }

    @Override
    void onAttached() {
        final String html = initialHtml();
        webView.loadDataWithBaseURL(
            HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl(),
            html,
            "text/html",
            "utf-8",
            null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(TYPE_EXTRA, mType.templateRes);
        outState.putSerializable(MAP_EXTRA, mMap);
    }

    private String initialHtml() {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(mType.templateRes);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return AffirmUtils.replacePlaceholders(html, mMap);
    }

    @Override
    public void onWebViewCancellation() {
        finish();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(mType.failureEvent, ERROR, null);
        finish();
    }
}



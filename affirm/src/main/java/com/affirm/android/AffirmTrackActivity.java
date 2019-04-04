package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.AffirmTrack;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.TRACK_ORDER_OBJECT;
import static com.affirm.android.AffirmConstants.TRACK_PRODUCT_OBJECT;
import static com.affirm.android.AffirmConstants.UTF_8;

public class AffirmTrackActivity extends AffirmActivity
        implements AffirmWebViewClient.WebViewClientCallbacks {

    private static final String AFFIRM_TRACK = "AFFIRM_TRACK";

    private AffirmTrack mAffirmTrack;

    private Handler mHandler = new Handler();

    static void startActivity(@NonNull Activity activity,
                              @NonNull AffirmTrack affirmTrack) {
        final Intent intent = new Intent(activity, AffirmTrackActivity.class);
        intent.putExtra(AFFIRM_TRACK, affirmTrack);
        activity.startActivity(intent);
    }

    @Override
    void initViews() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new TrackWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void beforeOnCreate() {
        AffirmUtils.hideActionBar(this);
    }

    @Override
    void initData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mAffirmTrack = savedInstanceState.getParcelable(AFFIRM_TRACK);
        } else {
            mAffirmTrack = getIntent().getParcelableExtra(AFFIRM_TRACK);
        }
    }

    @Override
    void onAttached() {
        final String html = initialHtml();
        webView.loadData(html, TEXT_HTML, UTF_8);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 10 * 1000);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(AFFIRM_TRACK, mAffirmTrack);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private String initialHtml() {
        String html;
        try {
            final InputStream ins =
                    getResources().openRawResource(R.raw.affirm_track_order_confirmed);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();

        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(TRACK_ORDER_OBJECT, buildOrderObject().toString());
        map.put(TRACK_PRODUCT_OBJECT, buildProductObject().toString());
        return AffirmUtils.replacePlaceholders(html, map);
    }

    private JsonObject buildOrderObject() {
        final JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(AffirmPlugins.get().gson()
                .toJson(mAffirmTrack.affirmTrackOrder())).getAsJsonObject();
    }

    private JsonArray buildProductObject() {
        final JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(AffirmPlugins.get().gson()
                .toJson(mAffirmTrack.affirmTrackProducts())).getAsJsonArray();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        finish();
    }
}

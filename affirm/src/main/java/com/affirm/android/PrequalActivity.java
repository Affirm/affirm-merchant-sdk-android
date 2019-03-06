package com.affirm.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

class PrequalActivity extends AppCompatActivity
        implements AffirmWebViewClient.Callbacks, AffirmWebChromeClient.Callbacks {
    private static final String PROTOCOL = "https://";
    private static final String REFERRING_URL = "https://androidsdk/";

    private static final String AMOUNT = "AMOUNT";
    private static final String PROMO_ID = "PROMO_ID";
    private static final String MAP_EXTRA = "MAP_EXTRA";

    private WebView webView;
    private View progressIndicator;

    private HashMap<String, String> map;

    static void startActivity(@NonNull Context context, float amount, @Nullable String promoId) {
        final Intent intent = new Intent(context, PrequalActivity.class);

        final HashMap<String, String> map = new HashMap<>();
        map.put(AMOUNT, String.valueOf(amount));
        map.put(PROMO_ID, promoId);

        intent.putExtra(MAP_EXTRA, map);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AffirmUtils.showCloseActionBar(this);

        if (savedInstanceState != null) {
            map = (HashMap<String, String>) savedInstanceState.getSerializable(MAP_EXTRA);
        } else {
            map = (HashMap<String, String>) getIntent().getSerializableExtra(MAP_EXTRA);
        }

        setContentView(R.layout.activity_webview);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        setupWebview();

        loadWebview();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(MAP_EXTRA, map);
    }

    private void setupWebview() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(new AffirmWebViewClient(this) {
            @Override
            boolean hasCallbackUrl(WebView view, String url) {
                if (url.equals(REFERRING_URL)) {
                    finish();
                    return true;
                }
                return false;
            }
        });
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    private void loadWebview() {
        String path;
        if (TextUtils.isEmpty(map.get(PROMO_ID))) {
            path = String.format(
                    "/apps/prequal?public_api_key=%s&unit_price=%s&isSDK=true&use_promo=True"
                            + "&referring_url=%s",
                    map.get(AffirmPlugins.get().publicKey()), map.get(AMOUNT), REFERRING_URL);
        } else {
            path = String.format(
                    "/apps/prequal?public_api_key=%s&unit_price=%s&promo_external_id=%s&isSDK=true"
                            + "&use_promo=True&referring_url=%s",
                    map.get(AffirmPlugins.get().publicKey()), map.get(AMOUNT), map.get(PROMO_ID), REFERRING_URL);
        }
        webView.loadUrl(PROTOCOL + AffirmPlugins.get().baseUrl() + path);
    }

    @Override
    public void onWebViewCancellation() {
        finish();
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        finish();
    }

    @Override
    public void onWebViewPageLoaded() {
        // ignore this
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

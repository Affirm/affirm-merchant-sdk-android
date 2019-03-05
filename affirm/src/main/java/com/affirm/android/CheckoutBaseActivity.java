package com.affirm.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;

import com.affirm.android.model.Checkout;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class CheckoutBaseActivity extends AppCompatActivity implements AffirmWebViewClient.Callbacks, PopUpWebChromeClient.Callbacks {

    public static final int RESULT_ERROR = -8575;

    public static final String CHECKOUT_ERROR = "checkout_error";

    static final String CHECKOUT_EXTRA = "checkout_extra";

    Checkout checkout;

    WebView webView;
    View progressIndicator;

    JsonObject buildJsonRequest(Checkout checkout, Merchant merchant) {
        final JsonObject configJson = new JsonObject();
        final JsonObject metadataJson = new JsonObject();
        final JsonObject jsonRequest = new JsonObject();
        final JsonParser jsonParser = new JsonParser();

        final JsonObject checkoutJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(checkout)).getAsJsonObject();
        final JsonObject merchantJson = jsonParser.parse(AffirmPlugins.get().gson().toJson(merchant)).getAsJsonObject();

        configJson.addProperty("user_confirmation_url_action", "GET");
        metadataJson.addProperty("platform_type", "Affirm Android SDK");
        metadataJson.addProperty("platform_affirm", BuildConfig.VERSION_NAME);

        checkoutJson.add("merchant", merchantJson);
        checkoutJson.add("config", configJson);
        checkoutJson.addProperty("api_version", "v2");
        checkoutJson.add("metadata", metadataJson);

        jsonRequest.add("checkout", checkoutJson);

        return jsonRequest;
    }

    abstract void startCheckout();

    abstract void setupWebView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.hideActionBar(this);

        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }

        setContentView(R.layout.activity_webview);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        setupWebView();

        startCheckout();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(CHECKOUT_EXTRA, checkout);
    }

    @Override
    protected void onDestroy() {
        clearCookies();
        webView.destroy();
        super.onDestroy();
    }

    public void clearCookies() {
        final CookieManager cookieManager = CookieManager.getInstance();
        final CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(this);
        CookiesUtil.clearCookieByUrl("https://" + AffirmPlugins.get().baseUrl(), cookieManager, cookieSyncManager);
    }

    @Override
    public void onWebViewError(@NonNull Throwable error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error.toString());
        setResult(RESULT_ERROR, intent);
        finish();
    }

    @Override
    public void onWebViewCancellation() {
        setResult(RESULT_CANCELED);
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
}

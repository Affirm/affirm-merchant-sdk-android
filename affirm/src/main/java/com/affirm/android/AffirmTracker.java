package com.affirm.android;

import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.sql.DriverManager.println;

class AffirmTracker {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final AtomicInteger localLogCounter = new AtomicInteger();
    private final String merchantKey;
    private final Affirm.Environment environment;

    public enum TrackingEvent {
        CHECKOUT_CREATION_FAIL("Checkout creation failed"),
        CHECKOUT_CREATION_SUCCESS("Checkout creation success"),
        CHECKOUT_WEBVIEW_SUCCESS("Checkout webView success"),
        CHECKOUT_WEBVIEW_FAIL("Checkout WebView failed"),
        VCN_CHECKOUT_CREATION_FAIL("Vcn Checkout creation failed"),
        VCN_CHECKOUT_CREATION_SUCCESS("Vcn Checkout creation success"),
        VCN_CHECKOUT_WEBVIEW_SUCCESS("Vcn Checkout webView success"),
        VCN_CHECKOUT_WEBVIEW_FAIL("Vcn Checkout webView failed"),
        PRODUCT_WEBVIEW_FAIL("Product webView failed"),
        SITE_WEBVIEW_FAIL("Site webView failed"),
        NETWORK_ERROR("network error");

        private final String name;

        TrackingEvent(String name) {
            this.name = name;
        }
    }

    enum TrackingLevel {
        INFO("info"), WARNING("warning"), ERROR("error");

        private final String level;

        TrackingLevel(String level) {
            this.level = level;
        }

        protected String getLevel() {
            return this.level;
        }
    }

    public AffirmTracker(@NonNull OkHttpClient client, @NonNull Affirm.Environment environment,
                         @NonNull String merchantKey) {
        this.client = client;
        this.merchantKey = merchantKey;
        this.environment = environment;
    }

    void track(@NonNull TrackingEvent event, @NonNull TrackingLevel level,
               @Nullable JsonObject data) {

        final String url = "https://" + environment.trackerBaseUrl + "/collect";

        final JsonObject json = addTrackingData(event.name, data, level);

        final RequestBody body = RequestBody.create(JSON, json.toString());

        final Request request = new Request.Builder().url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        final Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                println(e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                println(toString());
            }
        });
    }

    private JsonObject addTrackingData(@NonNull String eventName, @Nullable JsonObject eventData,
                                       @NonNull TrackingLevel level) {

        final Gson gson = new Gson();
        final JsonObject data = eventData == null ? new JsonObject()
                : gson.fromJson(gson.toJson(eventData, JsonObject.class), JsonObject.class);

        final long timeStamp = System.currentTimeMillis();
        // Set the log counter and then increment the logCounter
        data.addProperty("local_log_counter", localLogCounter.getAndIncrement());
        data.addProperty("ts", timeStamp);
        data.addProperty("event_name", eventName);
        data.addProperty("app_id", "Android SDK");
        data.addProperty("release", BuildConfig.VERSION_NAME);
        data.addProperty("android_sdk", Build.VERSION.SDK_INT);
        data.addProperty("device_name", Build.MODEL);
        data.addProperty("merchant_key", merchantKey);
        data.addProperty("level", level.getLevel());
        data.addProperty("environment", environment.name().toLowerCase());

        return data;
    }
}

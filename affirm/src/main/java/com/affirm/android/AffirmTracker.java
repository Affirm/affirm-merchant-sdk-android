package com.affirm.android;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.AffirmException;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

final class AffirmTracker {

    private static final AtomicInteger localLogCounter = new AtomicInteger();

    private AffirmTracker() {
    }

    enum TrackingEvent {
        CHECKOUT_CREATION_FAIL("Checkout creation failed"),
        CHECKOUT_CREATION_SUCCESS("Checkout creation success"),
        CHECKOUT_WEBVIEW_SUCCESS("Checkout webView success"),
        CHECKOUT_WEBVIEW_FAIL("Checkout WebView failed"),
        VCN_CHECKOUT_CREATION_FAIL("Vcn Checkout creation failed"),
        VCN_CHECKOUT_CREATION_SUCCESS("Vcn Checkout creation success"),
        VCN_CHECKOUT_WEBVIEW_SUCCESS("Vcn Checkout webView success"),
        VCN_CHECKOUT_WEBVIEW_FAIL("Vcn Checkout webView failed"),
        PREQUAL_WEBVIEW_FAIL("Prequal webView failed"),
        PRODUCT_WEBVIEW_FAIL("Product webView failed"),
        SITE_WEBVIEW_FAIL("Site webView failed"),
        NETWORK_ERROR("network error");

        private final String mName;

        TrackingEvent(String name) {
            mName = name;
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

    static void track(@NonNull TrackingEvent event, @NonNull TrackingLevel level,
                      @Nullable JsonObject data) {
        final JsonObject trackingData = addTrackingData(event.mName, data, level);
        new TrackerRequest(trackingData).create();
    }

    @VisibleForTesting
    @NonNull
    static JsonObject addTrackingData(@NonNull String eventName,
                                      @Nullable JsonObject eventData,
                                      @NonNull TrackingLevel level) {
        final Gson gson = new Gson();
        try {
            final JsonObject data = eventData == null ? new JsonObject()
                    : gson.fromJson(gson.toJson(eventData, JsonObject.class), JsonObject.class);

            fillTrackingData(eventName, data, level);
            return data;
        } catch (JsonSyntaxException | JsonIOException e) {
            return new JsonObject();
        }
    }

    @VisibleForTesting
    static void fillTrackingData(@NonNull String eventName,
                                 @NonNull JsonObject data,
                                 @NonNull AffirmTracker.TrackingLevel level) {
        final long timeStamp = System.currentTimeMillis();
        // Set the log counter and then increment the logCounter
        int localLogCounter = AffirmTracker.localLogCounter.getAndIncrement();
        data.addProperty("local_log_counter", localLogCounter);
        data.addProperty("ts", timeStamp);
        data.addProperty("app_id", "Android SDK");
        data.addProperty("release", BuildConfig.VERSION_NAME);
        data.addProperty("android_sdk", Build.VERSION.SDK_INT);
        data.addProperty("device_name", Build.MODEL);
        data.addProperty("merchant_key", AffirmPlugins.get().publicKey());
        data.addProperty("environment",
                AffirmPlugins.get().environmentName().toLowerCase(Locale.getDefault()));
        data.addProperty("event_name", eventName);
        data.addProperty("level", level.getLevel());
    }

    @NonNull
    static JsonObject createTrackingNetworkJsonObj(@NonNull Request request,
                                                   @Nullable Response response) {
        final JsonObject jsonObject = new JsonObject();
        final String affirmRequestIDHeader = "X-Affirm-Request-Id";
        jsonObject.addProperty("url", request.url().toString());
        jsonObject.addProperty("method", request.method());
        if (response != null) {
            final Headers headers = response.headers();
            jsonObject.addProperty("status_code", response.code());
            jsonObject.addProperty(affirmRequestIDHeader, headers.get(affirmRequestIDHeader));
            jsonObject.addProperty("x-amz-cf-id", headers.get("x-amz-cf-id"));
            jsonObject.addProperty("x-affirm-using-cdn", headers.get("x-affirm-using-cdn"));
            jsonObject.addProperty("x-cache", headers.get("x-cache"));
        } else {
            jsonObject.add("status_code", null);
            jsonObject.add(affirmRequestIDHeader, null);
        }
        return jsonObject;
    }

    @NonNull
    static JsonObject createTrackingExceptionJsonObj(@NonNull AffirmException exception) {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", exception.toString());
        return jsonObject;
    }
}

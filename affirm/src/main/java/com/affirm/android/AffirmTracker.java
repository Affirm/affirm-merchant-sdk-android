package com.affirm.android;

import android.os.AsyncTask;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static java.sql.DriverManager.println;

class AffirmTracker {

    enum TrackingEvent {
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

        private final String mName;

        TrackingEvent(String name) {
            this.mName = name;
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
        new TrackerTask(trackingData).execute();
    }

    private static JsonObject addTrackingData(@NonNull String eventName,
                                              @Nullable JsonObject eventData,
                                              @NonNull TrackingLevel level) {

        final Gson gson = new Gson();
        final JsonObject data = eventData == null ? new JsonObject()
            : gson.fromJson(gson.toJson(eventData, JsonObject.class), JsonObject.class);

        AffirmPlugins plugins = AffirmPlugins.get();
        plugins.addTrackingData(eventName, data, level);
        return data;
    }

    private static class TrackerTask extends AsyncTask<Void, Void, Void> {

        final JsonObject mTrackingData;

        TrackerTask(JsonObject trackingData) {
            this.mTrackingData = trackingData;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                AffirmApiHandler.sendTrackRequest(mTrackingData);
            } catch (IOException e) {
                e.printStackTrace();
                AffirmLog.e(toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            AffirmLog.d(toString());
        }
    }
}

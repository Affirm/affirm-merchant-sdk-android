package com.affirm.android;

import android.os.AsyncTask;

import com.google.gson.JsonObject;

import androidx.annotation.NonNull;

class TrackerRequest extends Request {

    private @NonNull
    JsonObject trackingData;

    TrackerRequest(@NonNull JsonObject trackingData) {
        this.trackingData = trackingData;
    }

    @Override
    void create() {
        requestCreate.create();
    }

    @Override
    void cancel() {
        requestCreate.cancel();
    }

    @Override
    AsyncTask createTask() {
        return new TrackerTask(trackingData);
    }

    @Override
    void cancelTask() {
        requestCreate.cancel();
    }

    private static class TrackerTask extends AsyncTask<Void, Void, Void> {

        @NonNull
        final JsonObject mTrackingData;

        TrackerTask(@NonNull JsonObject trackingData) {
            this.mTrackingData = trackingData;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                AffirmApiHandler.sendTrackRequest(mTrackingData);
            } catch (Exception e) {
                AffirmLog.e(toString());
            }
            return null;
        }
    }
}

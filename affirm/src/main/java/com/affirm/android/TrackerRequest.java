package com.affirm.android;

import android.os.AsyncTask;

import com.google.gson.JsonObject;

import androidx.annotation.NonNull;

class TrackerRequest extends AffirmRequest {

    @NonNull
    private JsonObject mTrackingData;

    TrackerRequest(@NonNull JsonObject trackingData) {
        mTrackingData = trackingData;
    }

    @Override
    AsyncTask createTask() {
        return new TrackerTask(mTrackingData);
    }

    private static class TrackerTask extends AsyncTask<Void, Void, Void> {

        @NonNull
        private final JsonObject mTrackingData;

        TrackerTask(@NonNull JsonObject trackingData) {
            this.mTrackingData = trackingData;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                AffirmApiHandler.sendTrackRequest(mTrackingData);
            } catch (Exception e) {
                AffirmLog.w(e.toString());
            }
            return null;
        }
    }
}

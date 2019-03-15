package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.google.gson.JsonObject;

import java.io.IOException;

import androidx.annotation.NonNull;

class TrackerRequest extends Request {

    void create(@NonNull JsonObject trackingData) {
        trackerCreator.create(trackingData);
    }

    interface TrackerCreator {

        void create(@NonNull JsonObject trackingData);
    }

    private final TrackerCreator trackerCreator = new TrackerCreator() {
        @Override
        public void create(@NonNull JsonObject trackingData) {
            new TrackerTask(trackingData).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

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
            } catch (IOException e) {
                AffirmLog.e(toString());
            } catch (APIException e) {
                AffirmLog.e(toString());
            } catch (PermissionException e) {
                AffirmLog.e(toString());
            } catch (InvalidRequestException e) {
                AffirmLog.e(toString());
            }
            return null;
        }
    }
}

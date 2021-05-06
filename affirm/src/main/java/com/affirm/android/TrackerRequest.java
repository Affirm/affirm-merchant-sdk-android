package com.affirm.android;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import okhttp3.Call;

import static com.affirm.android.AffirmConstants.HTTP;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;

class TrackerRequest implements AffirmRequest {

    @NonNull
    private JsonObject trackingData;

    private Call trackingCall;

    TrackerRequest(@NonNull JsonObject trackingData) {
        this.trackingData = trackingData;
    }

    @Override
    public void create() {

        if (trackingCall != null) {
            trackingCall.cancel();
        }

        trackingCall = AffirmClient.send(new AffirmTrackerRequest(),
                new AffirmClient.AffirmListener<Void>() {
            @Override
            public void onSuccess(Void response) {

            }

            @Override
            public void onFailure(AffirmException exception) {
                handleException(exception);
            }
        });
    }

    @Override
    public void cancel() {
        if (trackingCall != null) {
            trackingCall.cancel();
            trackingCall = null;
        }
    }

    private String getTrackerProtocol() {
        return AffirmPlugins.get().trackerBaseUrl().contains(HTTP) ? "" : HTTPS_PROTOCOL;
    }

    private void handleException(AffirmException e) {
        AffirmLog.w(e.toString());
    }


    class AffirmTrackerRequest implements AffirmClient.AffirmApiRequest {

        @NotNull
        @Override
        public String url() {
            return getTrackerProtocol()
                    +  AffirmPlugins.get().trackerBaseUrl()
                    + TRACKER_PATH;
        }

        @NotNull
        @Override
        public AffirmHttpRequest.Method method() {
            return AffirmHttpRequest.Method.POST;
        }

        @Nullable
        @Override
        public JsonObject body() {
            return trackingData;
        }
    }
}

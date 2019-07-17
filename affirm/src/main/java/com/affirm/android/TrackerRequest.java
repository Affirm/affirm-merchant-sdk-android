package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.AffirmError;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.CONTENT_TYPE;
import static com.affirm.android.AffirmConstants.HTTP;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.TAG_TRACKER;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;
import static com.affirm.android.AffirmConstants.X_AFFIRM_REQUEST_ID;

class TrackerRequest implements AffirmRequest {

    @NonNull
    private JsonObject trackingData;

    private Call trackingCall;

    TrackerRequest(@NonNull JsonObject trackingData) {
        this.trackingData = trackingData;
    }

    @Override
    public void create() {
        AffirmPlugins plugins = AffirmPlugins.get();

        if (trackingCall != null) {
            trackingCall.cancel();
        }

        trackingCall = plugins.restClient().getCallForRequest(
                new AffirmHttpRequest.Builder()
                        .setUrl(getTrackerProtocol() + plugins.trackerBaseUrl() + TRACKER_PATH)
                        .setMethod(AffirmHttpRequest.Method.POST)
                        .setBody(new AffirmHttpBody(CONTENT_TYPE, trackingData.toString()))
                        .setTag(TAG_TRACKER)
                        .build()
        );
        trackingCall.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (!response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null && responseBody.contentLength() > 0) {
                        final AffirmError affirmError = AffirmPlugins.get().gson().fromJson(responseBody.charStream(), AffirmError.class);

                        AffirmException affirmException = AffirmHttpClient.handleAPIError(
                                affirmError,
                                response.code(),
                                response.headers().get(X_AFFIRM_REQUEST_ID)
                        );

                        handleException(affirmException);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                handleException(new ConnectionException("i/o failure", e));
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
}

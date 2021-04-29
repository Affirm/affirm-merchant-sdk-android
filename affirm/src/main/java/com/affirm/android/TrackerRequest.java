package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.TRACKER_PATH;

class TrackerRequest implements AffirmRequest, AffirmApiRepository.AffirmApiListener<String> {

    private final AffirmApiRepository repository;
    @NonNull
    private final JsonObject trackingData;

    TrackerRequest(@NonNull JsonObject trackingData) {
        this.trackingData = trackingData;
        this.repository = new AffirmApiRepository();
    }

    @Override
    public void create() {
        AffirmPlugins plugins = AffirmPlugins.get();
        final String url = HTTPS_PROTOCOL + plugins.trackerBaseUrl() + TRACKER_PATH;
        this.repository.trackerRequest(url, trackingData.toString(), this);
    }

    @Override
    public void cancel() {
        this.repository.cancelRequest();
    }

    @Override
    public void onResponse(@NotNull String response) {
        AffirmLog.d("Tracking successful, Response: " + response);
    }

    @Override
    public void onFailed(@NotNull AffirmException exception) {
        handleException(exception);
    }

    private void handleException(AffirmException e) {
        AffirmLog.w(e.toString());
    }
}

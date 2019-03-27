package com.affirm.android;

import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AffirmTrackerTest {
    private static AtomicInteger mLocalLogCounter;

    @Before
    public void init(){
        mLocalLogCounter = new AtomicInteger();
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("sdf", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }


    @Test
    public void testTrack(){
        JsonObject data = addTrackingData(AffirmTracker.TrackingEvent.CHECKOUT_CREATION_FAIL.name(),
                null, AffirmTracker.TrackingLevel.INFO);
        assertNotEquals(-1,data.get("local_log_counter").getAsInt());
        assertNotEquals(0L,data.get("ts").getAsLong());
        assertEquals("Android SDK",data.get("app_id").getAsString());
        assertEquals(BuildConfig.VERSION_NAME,data.get("release").getAsString());
        assertEquals(Build.VERSION.SDK_INT,data.get("android_sdk").getAsInt());
        assertNotEquals(null,data.get("device_name"));
        assertNotEquals(null,data.get("merchant_key").getAsString());
        assertNotEquals(null,data.get("environment").getAsString());
        assertEquals(AffirmTracker.TrackingEvent.CHECKOUT_CREATION_FAIL.name(),data.get("event_name").getAsString());
        assertEquals(AffirmTracker.TrackingLevel.INFO.getLevel(),data.get("level").getAsString());
    }

    @NonNull
    private static JsonObject addTrackingData(@NonNull String eventName,
                                              @Nullable JsonObject eventData,
                                              @NonNull AffirmTracker.TrackingLevel level) {

        final Gson gson = new Gson();
        final JsonObject data = eventData == null ? new JsonObject()
                : gson.fromJson(gson.toJson(eventData, JsonObject.class), JsonObject.class);

        fillTrackingData(eventName, data, level);
        return data;
    }

    private static void fillTrackingData(@NonNull String eventName,
                                         @NonNull JsonObject data,
                                         @NonNull AffirmTracker.TrackingLevel level) {
        final long timeStamp = System.currentTimeMillis();
        // Set the log counter and then increment the logCounter
        int localLogCounter = mLocalLogCounter.getAndIncrement();
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
}

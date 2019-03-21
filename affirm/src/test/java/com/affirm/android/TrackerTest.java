package com.affirm.android;

import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;

@RunWith(RobolectricTestRunner.class)
public class TrackerTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder()
                    .setPublicKey("sdf")
                    .build()
            );
        }
    }

    @Test
    public void testGetNewPromo() {
        final JsonObject data = new JsonObject();
        data.addProperty("a", 1);
        data.addProperty("b", "b");
        AffirmTracker.track(NETWORK_ERROR, ERROR, data);

        new TrackerRequest(data).create();
    }

}

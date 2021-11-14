package com.affirm.android;

import com.google.common.truth.Truth;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@RunWith(RobolectricTestRunner.class)
public class TrackerRequestTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testAddTrackingData() {
        final JsonObject data = new JsonObject();
        data.addProperty("a", 1);
        data.addProperty("b", "b");

        JsonObject trackData = AffirmTracker.addTrackingData(NETWORK_ERROR.name(), data, ERROR);

        Truth.assertThat(trackData.get("a").getAsInt() == 1).isTrue();
        Truth.assertThat(trackData.get("b").getAsString().equals("b")).isTrue();
    }

    @Test
    public void testFillTrackingData() {
        final JsonObject data = new JsonObject();
        data.addProperty("a", 1);
        data.addProperty("b", "b");

        String eventName = NETWORK_ERROR.name();
        AffirmTracker.TrackingLevel level = ERROR;

        AffirmTracker.fillTrackingData(eventName, data, level);

        Truth.assertThat(data.get("app_id").getAsString().equals("Android SDK")).isTrue();
        Truth.assertThat(data.get("release").getAsString().equals(BuildConfig.VERSION_NAME)).isTrue();
        Truth.assertThat(data.get("merchant_key").getAsString().equals(AffirmPlugins.get().publicKey())).isTrue();
        Truth.assertThat(data.get("environment").getAsString().equals(AffirmPlugins.get().environmentName().toLowerCase(Locale.getDefault()))).isTrue();
        Truth.assertThat(data.get("event_name").getAsString().equals(eventName)).isTrue();
        Truth.assertThat(data.get("level").getAsString().equals(level.getLevel())).isTrue();
    }

    @Test
    public void testCreateTrackingNetworkJsonObj() {
        String url = "https://www.google.com/";

        Request request = new Request.Builder()
                .url(url)
                .build();

        JsonObject trackData = AffirmTracker.createTrackingNetworkJsonObj(request, null);

        Truth.assertThat(trackData.get("url").getAsString().equals(url)).isTrue();
    }

    @Test
    public void testTrack() throws Exception {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        final JsonObject data = new JsonObject();
        data.addProperty("a", 1);
        data.addProperty("b", "b");

        final JsonObject trackingData = AffirmTracker.addTrackingData(NETWORK_ERROR.name(), data, ERROR);
        new TrackerRequest(client, trackingData).create();

        Mockito.verify(client, times(1)).newCall(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        final String body = RequestUtils.bodyToString(request);

        JsonObject jsonObject = new JsonParser().parse(body).getAsJsonObject();

        Truth.assertThat(jsonObject.get("a").getAsInt() == 1).isTrue();
        Truth.assertThat(jsonObject.get("b").getAsString().equals("b")).isTrue();
        Truth.assertThat(jsonObject.get("event_name").getAsString().equals("NETWORK_ERROR")).isTrue();
        Truth.assertThat(jsonObject.get("app_id").getAsString().equals("Android SDK")).isTrue();
        Truth.assertThat(jsonObject.get("release").getAsString().equals(BuildConfig.VERSION_NAME)).isTrue();
        Truth.assertThat(jsonObject.get("device_name").getAsString().equals("robolectric")).isTrue();
        Truth.assertThat(jsonObject.get("merchant_key").getAsString().equals("Y8CQXFF044903JC0")).isTrue();
        Truth.assertThat(jsonObject.get("level").getAsString().equals("error")).isTrue();
        Truth.assertThat(jsonObject.get("environment").getAsString().equals("sandbox")).isTrue();

        Mockito.verify(call).enqueue(any(Callback.class));
    }
}

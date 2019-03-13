package com.affirm.android;

import com.google.common.truth.Truth;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;

import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TrackerTest {

    @Mock
    OkHttpClient client;
    @Mock
    Call call;
    @Captor
    ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder()
                .setPublicKey("sdf")
                .build()
            );
        }
    }

    @Test
    public void track() throws Exception {
        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

        final JsonObject data = new JsonObject();
        data.addProperty("a", 1);
        data.addProperty("b", "b");
        AffirmTracker.track(NETWORK_ERROR, ERROR, data);

        Mockito.verify(client, times(1)).newCall(requestCaptor.capture());
        final Request request = requestCaptor.getValue();
        String body = bodyToString(request);
        body = body.replace(body.substring(body.indexOf("\"ts\":") + "\"ts\":".length(), body.indexOf(",\"event_name\"")), "1498081546783");

        Truth.assertThat(body)
                .isEqualTo("{\"a\":1,\"b\":\"b\",\"local_log_counter\":0,\"ts\":1498081546783,"
                        + "\"event_name\":\"network error\",\"app_id\":\"Android SDK\",\"release\":\""
                        + BuildConfig.VERSION_NAME
                        + "\",\"android_sdk\":0,\"device_name\":null,"
                        + "\"merchant_key\":\"111\",\"level\":\"error\","
                        + "\"environment\":\"sandbox\"}");

        Mockito.verify(call).enqueue(any(Callback.class));
    }

    private static String bodyToString(final Request request) throws IOException {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        copy.body().writeTo(buffer);
        return buffer.readUtf8();
    }
}
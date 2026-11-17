package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.google.gson.JsonObject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
public class AffirmClientTest {

    private static final String ERROR_RESPONSE = "{\"status_code\":400,\"type\":\"invalid_request\",\"code\":\"invalid_field\",\"field\":\"shipping.address\",\"message\":\"Your shipping address is invalid.\",\"ui\":{\"sub\":\"Please return to Your Customer-Facing Merchant Name to correct the shipping address you entered:\",\"main\":\"We could not validate your shipping address\",\"sub_extra\":[\"123 Fake St\",\"San Chicago, WA 11223\"]}}";

    private MockWebServer server;

    @Before
    public void setup() throws Exception {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void send_PreservesStructuredApiError() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(AffirmConstants.X_AFFIRM_REQUEST_ID, "req_123")
                .setBody(ERROR_RESPONSE));

        AtomicReference<AffirmException> error = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        AffirmClient.send(new TestRequest(server.url("/checkout").toString()),
                new AffirmClient.AffirmListener<JsonObject>() {
                    @Override
                    public void onSuccess(JsonObject response) {
                    }

                    @Override
                    public void onFailure(AffirmException exception) {
                        error.set(exception);
                        latch.countDown();
                    }
                });

        for (int i = 0; i < 20 && latch.getCount() > 0; i++) {
            latch.await(50, TimeUnit.MILLISECONDS);
            ShadowLooper.runUiThreadTasks();
        }

        Assert.assertNotNull(error.get());
        Assert.assertTrue(error.get() instanceof APIException);
        Assert.assertNull(error.get().getAffirmError());
        Assert.assertTrue(error.get().getCause() instanceof AffirmException);

        AffirmException cause = (AffirmException) error.get().getCause();
        Assert.assertEquals("req_123", cause.getRequestId());
        Assert.assertEquals((Integer) 400, cause.getStatusCode());
        Assert.assertNotNull(cause.getAffirmError());
        Assert.assertNotNull(cause.getAffirmError().ui());
        Assert.assertEquals("We could not validate your shipping address",
                cause.getAffirmError().ui().main());
        Assert.assertEquals("123 Fake St", cause.getAffirmError().ui().subExtra().get(0));
    }

    private static class TestRequest implements AffirmClient.AffirmApiRequest {

        private final String url;

        TestRequest(String url) {
            this.url = url;
        }

        @NonNull
        @Override
        public String url() {
            return url;
        }

        @NonNull
        @Override
        public AffirmHttpRequest.Method method() {
            return AffirmHttpRequest.Method.GET;
        }

        @Nullable
        @Override
        public JsonObject body() {
            return null;
        }

        @Nullable
        @Override
        public Map<String, String> headers() {
            return null;
        }
    }
}

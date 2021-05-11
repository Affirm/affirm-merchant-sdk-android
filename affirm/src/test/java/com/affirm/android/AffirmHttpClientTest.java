package com.affirm.android;

import com.google.gson.JsonObject;

import org.junit.Test;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AffirmHttpClientTest {

    @Test
    public void testOkHttpClientExecuteWithSuccessResponse() throws Exception {
        execute(200, "OK", "Success", new AffirmHttpClient(null));
    }

    @Test
    public void testOkHttpClientExecuteWithErrorResponse() throws Exception {
        execute(404, "NOT FOUND", "Error", new AffirmHttpClient(null));
    }

    private void execute(int responseCode, String responseStatus,
                         String responseContent, AffirmHttpClient client) throws Exception {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        MockWebServer server = new MockWebServer();

        // Make mock response
        int responseContentLength = responseContent.length();
        MockResponse mockResponse = new MockResponse()
                .setStatus("HTTP/1.1 " + responseCode + " " + responseStatus)
                .setBody(responseContent);

        // Start mock server
        server.enqueue(mockResponse);
        server.start();

        String requestUrl = server.url("/").toString();
        JsonObject json = new JsonObject();
        json.addProperty("key", "value");
        String requestContent = json.toString();
        int requestContentLength = requestContent.length();
        String requestContentType = "application/json";
        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(requestUrl)
                .setMethod(AffirmHttpRequest.Method.POST)
                .setBody(new AffirmHttpBody(requestContentType, requestContent))
                .build();

        // Execute request
        Call okHttpCall = client.getCallForRequest(okHttpClient, request);
        Response response = okHttpCall.execute();

        RecordedRequest recordedApacheRequest = server.takeRequest();

        // Verify request method
        assertEquals(AffirmHttpRequest.Method.POST.toString(), recordedApacheRequest.getMethod());
        // Verify request body
        assertEquals(requestContentLength, recordedApacheRequest.getBodySize());

        // Verify response status code
        assertEquals(responseCode, response.code());
        // Verify response body
        byte[] content = response.body().bytes();
        assertArrayEquals(responseContent.getBytes(), content);
        // Verify response body size
        assertEquals(responseContentLength, content.length);

        // Shutdown mock server
        server.shutdown();
    }
}
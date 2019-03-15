package com.affirm.android;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.AffirmError;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;

import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;

final class AffirmHttpClient {

    private OkHttpClient okHttpClient;

    private AffirmHttpClient(@Nullable OkHttpClient.Builder builder) {
        if (builder == null) {
            builder = new OkHttpClient.Builder();
        }

        okHttpClient = builder.build();
    }

    static AffirmHttpClient createClient(@Nullable OkHttpClient.Builder builder) {
        return new AffirmHttpClient(builder);
    }

    AffirmHttpResponse execute(final AffirmHttpRequest request) throws APIException,
            PermissionException, InvalidRequestException, ConnectionException {
        return execute(request, true);
    }

    AffirmHttpResponse execute(final AffirmHttpRequest request, boolean sendTrackEvent)
            throws APIException, PermissionException, InvalidRequestException, ConnectionException {
        Request okHttpRequest = getRequest(request);
        Call call = okHttpClient.newCall(okHttpRequest);
        try {
            Response response = call.execute();

            boolean responseSuccess = response.isSuccessful();
            if (!responseSuccess && sendTrackEvent) {
                AffirmTracker.get().track(NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(okHttpRequest, response));
            }

            final Headers headers = response.headers();
            String requestId = headers.get("X-Affirm-Request-Id");
            if (response.code() < 200 || response.code() >= 300) {
                final AffirmError affirmError = AffirmPlugins.get().gson()
                        .fromJson(response.body().string(), AffirmError.class);
                handleAPIError(affirmError, response.code(), requestId);
            }

            return getResponse(response);

        } catch (IOException e) {
            if (sendTrackEvent) {
                AffirmTracker.get().track(NETWORK_ERROR, ERROR,
                        createTrackingNetworkJsonObj(okHttpRequest, null));
            }

            throw new ConnectionException("i/o failure", e);
        }
    }

    private static void handleAPIError(@NonNull AffirmError affirmError, int responseCode,
                                       @Nullable String requestId) throws APIException,
            PermissionException, InvalidRequestException {

        switch (responseCode) {
            case 400:
            case 404: {
                throw new InvalidRequestException(
                        affirmError.message(),
                        affirmError.type(),
                        affirmError.field(),
                        requestId,
                        affirmError.status(),
                        affirmError,
                        null);
            }
            case 403: {
                throw new PermissionException(affirmError.message(), requestId, responseCode,
                        affirmError);
            }
            default: {
                throw new APIException(affirmError.message(), requestId, responseCode, affirmError,
                        null);
            }
        }
    }

    void cancelCallWithTag(String tag) {
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            Object requestTag = call.request().tag();
            if (requestTag != null && requestTag.equals(tag)) {
                call.cancel();
            }
        }
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            Object requestTag = call.request().tag();
            if (requestTag != null && requestTag.equals(tag)) {
                call.cancel();
            }
        }
    }

    private AffirmHttpResponse getResponse(Response response) throws IOException {
        // Status code
        int statusCode = response.code();
        // Content
        String content = null;
        // Total size
        int totalSize = 0;
        // Content type
        String contentType = null;

        ResponseBody body = response.body();
        if (body != null) {
            content = body.string();
            totalSize = (int) body.contentLength();

            MediaType mediaType = body.contentType();
            if (mediaType != null) {
                contentType = mediaType.toString();
            }
        }
        return new AffirmHttpResponse.Builder()
                .setStatusCode(statusCode)
                .setContent(content)
                .setTotalSize(totalSize)
                .setContentType(contentType)
                .build();
    }

    private Request getRequest(AffirmHttpRequest request) {
        Request.Builder okHttpRequestBuilder = new Request.Builder();
        AffirmHttpRequest.Method method = request.getMethod();
        // Set method
        switch (method) {
            case GET:
                okHttpRequestBuilder.get();
                break;
            case DELETE:
            case POST:
            case PUT:
                // Since we need to set body and method at the same time for DELETE, POST, PUT,
                // we will do it in
                // the following.
                break;
            default:
                // This case will never be reached since we have already handled this case in
                throw new IllegalStateException("Unsupported http method " + method.toString());
        }
        // Set url
        okHttpRequestBuilder.url(request.getUrl());

        // Set Body
        AffirmHttpBody body = request.getBody();
        AffirmOkHttpRequestBody okHttpRequestBody = null;
        if (body != null) {
            okHttpRequestBody = new AffirmOkHttpRequestBody(body);
        }

        // set tag
        okHttpRequestBuilder.tag(request.getTag());

        if (okHttpRequestBody != null) {
            switch (method) {
                case PUT:
                    okHttpRequestBuilder.put(okHttpRequestBody);
                    break;
                case POST:
                    okHttpRequestBuilder.post(okHttpRequestBody);
                    break;
                case DELETE:
                    okHttpRequestBuilder.delete(okHttpRequestBody);
                default:
                    break;
            }
        }
        return okHttpRequestBuilder.build();
    }

    private static class AffirmOkHttpRequestBody extends RequestBody {

        private AffirmHttpBody body;
        private byte[] content;
        private int offset;
        private int byteCount;

        AffirmOkHttpRequestBody(AffirmHttpBody body) {
            this.body = body;
            this.content = body.getContent().getBytes(Util.UTF_8);
            this.offset = 0;
            this.byteCount = content.length;
        }

        @Override
        public long contentLength() {
            return byteCount;
        }

        @Override
        public MediaType contentType() {
            String contentType = body.getContentType();
            return contentType == null ? null : MediaType.parse(body.getContentType());
        }

        @Override
        public void writeTo(@NonNull BufferedSink sink) throws IOException {
            sink.write(content, offset, byteCount);
        }
    }

}

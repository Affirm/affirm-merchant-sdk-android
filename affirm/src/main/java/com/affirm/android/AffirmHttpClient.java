package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.AffirmError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;

import static com.affirm.android.AffirmConstants.X_AFFIRM_REQUEST_ID;
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

    OkHttpClient getOkHttpClientt() {
        return okHttpClient;
    }

    static AffirmException handleAPIError(
            @NonNull AffirmError affirmError,
            int responseCode,
            @Nullable String requestId
    ) {
        switch (responseCode) {
            case 400:
            case 404: {
                return new InvalidRequestException(
                        affirmError.message(),
                        affirmError.type(),
                        affirmError.field(),
                        requestId,
                        affirmError.status(),
                        affirmError,
                        null
                );
            }
            case 403: {
                return new PermissionException(
                        affirmError.message(),
                        requestId,
                        responseCode,
                        affirmError
                );
            }
            default: {
                return new APIException(
                        affirmError.message(),
                        requestId,
                        responseCode,
                        affirmError,
                        null
                );
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

    Request getRequest(AffirmHttpRequest request) {
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
                break;
            default:
                // This case will never be reached since we have already handled this case in
                throw new IllegalStateException("Unsupported http method: " + method.toString());
        }
        // Set request url
        okHttpRequestBuilder.url(request.getUrl());

        // Set request body
        AffirmHttpBody body = request.getBody();
        AffirmOkHttpRequestBody okHttpRequestBody = null;
        if (body != null) {
            okHttpRequestBody = new AffirmOkHttpRequestBody(body);
        }

        // set request tag
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
                    break;
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
            this.content = body.getContent().getBytes(StandardCharsets.UTF_8);
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

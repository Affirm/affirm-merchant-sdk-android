package com.affirm.android;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;

class AffirmHttpClient {

    private OkHttpClient mOkHttpClient;
    private Call mCall;

    private AffirmHttpClient(@Nullable OkHttpClient.Builder builder) {
        if (builder == null) {
            builder = new OkHttpClient.Builder();
        }

        mOkHttpClient = builder.build();
    }

    static AffirmHttpClient createClient(@Nullable OkHttpClient.Builder builder) {
        return new AffirmHttpClient(builder);
    }

    AffirmHttpResponse execute(final AffirmHttpRequest request) throws IOException {
        Request okHttpRequest = getRequest(request);
        mCall = mOkHttpClient.newCall(okHttpRequest);
        Response response = mCall.execute();
        return getResponse(response);
    }

    void cancel() {
        if (mCall != null) {
            mCall.cancel();
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

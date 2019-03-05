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

class AffirmHttpClient<T> {

    private OkHttpClient okHttpClient;

    interface Callback<T> {
        void onSuccess(T result);

        void onFailure(Throwable throwable);
    }

    private AffirmHttpClient(@Nullable OkHttpClient.Builder builder) {
        if (builder == null) {
            builder = new OkHttpClient.Builder();
        }

        okHttpClient = builder.build();
    }

    static AffirmHttpClient createClient(@Nullable OkHttpClient.Builder builder) {
        return new AffirmHttpClient(builder);
    }

    AffirmHttpResponse execute(final AffirmHttpRequest request) throws IOException {
        Request okHttpRequest = getRequest(request);
        Call call = okHttpClient.newCall(okHttpRequest);
        Response response = call.execute();
        return getResponse(response);

//        call.enqueue(new okhttp3.Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                callback.onFailure(e);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    if (response.code() == 403) {
//                        callback.onFailure(
//                                new Exception("Got error for request: " + response.code()));
//                    } else if (response.code() >= 400 && response.code() < 500 && response.code() != 404) {
//                        final ErrorResponse errorResponse =
//                                AffirmPlugins.get().gson().fromJson(response.body().string(), ErrorResponse.class);
//
//                        callback.onFailure(new AffirmError(errorResponse));
//                    } else {
//                        callback.onFailure(
//                                new Exception("Got error for request: " + response.code()));
//                    }
//
//                    return;
//                }
//                final String bodyString = response.body().string();
//                final T res = AffirmPlugins.get().gson().fromJson(bodyString, clazz);
//                callback.onSuccess(res);
//            }
//        });
    }

    private AffirmHttpResponse getResponse(Response response) throws IOException {
        // Status code
        int statusCode = response.code();

        // Content
        String content = response.body().string();

        // Total size
        int totalSize = (int) response.body().contentLength();

        // Content type
        String contentType = null;
        ResponseBody body = response.body();
        if (body != null && body.contentType() != null) {
            contentType = body.contentType().toString();
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
                // Since we need to set body and method at the same time for DELETE, POST, PUT, we will do it in
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

package com.affirm.android.utils;

import java.io.IOException;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.Okio;
import okio.Source;
import okio.Timeout;

public class ResponseFactory {
    public static <T> Response success(String body) {
        return new Response.Builder().code(200)
                .message("OK")
                .addHeader("X-Affirm-Request-Id", "requestId")
                .protocol(Protocol.HTTP_1_1)
                .body(responseBody(body))
                .request(new Request.Builder().url("http://localhost/").build())
                .build();
    }

    public static Response error(int code, String body) {
        return new Response.Builder().message("")
                .code(code)
                .addHeader("X-Affirm-Request-Id", "requestId")
                .protocol(Protocol.HTTP_1_1)
                .body(responseBody(body))
                .request(new Request.Builder().url("http://localhost/").build())
                .build();
    }

    private static ResponseBody responseBody(String content) {
        final Buffer data = new Buffer().writeUtf8(content);

        Source source = new Source() {
            boolean closed;

            @Override
            public void close() throws IOException {
                closed = true;
            }

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                if (closed) throw new IllegalStateException();
                return data.read(sink, byteCount);
            }

            @Override
            public Timeout timeout() {
                return Timeout.NONE;
            }
        };

        return ResponseBody.create(null, -1, Okio.buffer(source));
    }
}

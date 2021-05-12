package com.affirm.android;

import java.util.Objects;

import okhttp3.Request;
import okio.Buffer;

public class RequestUtils {

    public static String bodyToString(final Request request) throws Exception {
        final Request copy = request.newBuilder().build();
        final Buffer buffer = new Buffer();
        Objects.requireNonNull(copy.body()).writeTo(buffer);
        return buffer.readUtf8();
    }
}

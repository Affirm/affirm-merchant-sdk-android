package com.affirm.android;

import java.util.HashMap;
import java.util.Map;

public final class AffirmHttpRequest {
    private final String url;
    private final Method method;
    private final AffirmHttpBody body;
    private final String tag;
    private final Map<String, String> headers;

    private AffirmHttpRequest(Builder builder) {
        url = builder.mUrl;
        method = builder.mMethod;
        body = builder.mBody;
        tag = builder.mTag;
        headers = builder.headers;
    }

    String getUrl() {
        return url;
    }

    Method getMethod() {
        return method;
    }


    Map<String, String> getAllHeaders() {
        return headers;
    }


    AffirmHttpBody getBody() {
        return body;
    }

    String getTag() {
        return tag;
    }

    public enum Method {
        GET, POST, PUT, DELETE
    }

    static final class Builder {

        private String mUrl;
        private Method mMethod;
        private AffirmHttpBody mBody;
        private String mTag;
        private Map<String, String> headers;

        Builder() {
        }

        public Builder setUrl(String url) {
            mUrl = url;
            return this;
        }


        Builder setMethod(AffirmHttpRequest.Method method) {
            mMethod = method;
            return this;
        }

        Builder setBody(AffirmHttpBody body) {
            mBody = body;
            return this;
        }

        Builder setTag(String tag) {
            mTag = tag;
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }

        AffirmHttpRequest build() {
            return new AffirmHttpRequest(this);
        }
    }
}

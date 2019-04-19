package com.affirm.android;

final class AffirmHttpRequest {
    private final String url;
    private final Method method;
    private final AffirmHttpBody body;
    private final String tag;

    private AffirmHttpRequest(Builder builder) {
        url = builder.mUrl;
        method = builder.mMethod;
        body = builder.mBody;
        tag = builder.mTag;
    }

    String getUrl() {
        return url;
    }

    Method getMethod() {
        return method;
    }

    AffirmHttpBody getBody() {
        return body;
    }

    String getTag() {
        return tag;
    }

    enum Method {
        GET, POST, PUT, DELETE
    }

    static final class Builder {

        private String mUrl;
        private Method mMethod;
        private AffirmHttpBody mBody;
        private String mTag;

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

        AffirmHttpRequest build() {
            return new AffirmHttpRequest(this);
        }
    }
}

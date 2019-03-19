package com.affirm.android;

final class AffirmHttpRequest {
    private final String mUrl;
    private final Method mMethod;
    private final AffirmHttpBody mBody;
    private final String mTag;

    private AffirmHttpRequest(Builder builder) {
        mUrl = builder.mUrl;
        mMethod = builder.mMethod;
        mBody = builder.mBody;
        mTag = builder.mTag;
    }

    String getUrl() {
        return mUrl;
    }

    Method getMethod() {
        return mMethod;
    }

    AffirmHttpBody getBody() {
        return mBody;
    }

    String getTag() {
        return mTag;
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

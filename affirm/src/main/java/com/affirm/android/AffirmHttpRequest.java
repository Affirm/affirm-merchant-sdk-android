package com.affirm.android;

final class AffirmHttpRequest {
    private final String url;
    private final Method method;
    private final AffirmHttpBody body;
    private final String tag;

    private AffirmHttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.body = builder.body;
        this.tag = builder.tag;
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

        private String url;
        private Method method;
        private AffirmHttpBody body;
        private String tag;

        Builder() {
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }


        Builder setMethod(AffirmHttpRequest.Method method) {
            this.method = method;
            return this;
        }

        Builder setBody(AffirmHttpBody body) {
            this.body = body;
            return this;
        }

        Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        AffirmHttpRequest build() {
            return new AffirmHttpRequest(this);
        }
    }
}

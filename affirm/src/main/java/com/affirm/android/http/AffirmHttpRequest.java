package com.affirm.android.http;

public final class AffirmHttpRequest {
    private final String url;
    private final Method method;
    private final AffirmHttpBody body;

    private AffirmHttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.body = builder.body;
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public AffirmHttpBody getBody() {
        return body;
    }

    public enum Method {
        GET, POST, PUT, DELETE
    }

    public static final class Builder {

        private String url;
        private Method method;
        private AffirmHttpBody body;

        public Builder() {
        }

        public Builder(AffirmHttpRequest request) {
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }


        public Builder setMethod(AffirmHttpRequest.Method method) {
            this.method = method;
            return this;
        }

        public Builder setBody(AffirmHttpBody body) {
            this.body = body;
            return this;
        }

        public AffirmHttpRequest build() {
            return new AffirmHttpRequest(this);
        }
    }
}

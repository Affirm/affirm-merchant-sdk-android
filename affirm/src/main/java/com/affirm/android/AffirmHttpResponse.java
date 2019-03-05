package com.affirm.android;

final class AffirmHttpResponse {
    private final int statusCode;
    private final String content;
    private final long totalSize;
    private final String contentType;

    private AffirmHttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.content = builder.content;
        this.totalSize = builder.totalSize;
        this.contentType = builder.contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContent() {
        return content;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String getContentType() {
        return contentType;
    }

    public static final class Builder {

        private int statusCode;
        private String content;
        private long totalSize;
        private String contentType;

        public Builder() {
            this.totalSize = -1;
        }

        public Builder(AffirmHttpResponse response) {
            super();
            this.setStatusCode(response.getStatusCode());
            this.setContent(response.getContent());
            this.setTotalSize(response.getTotalSize());
            this.setContentType(response.getContentType());
        }

        public Builder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setTotalSize(long totalSize) {
            this.totalSize = totalSize;
            return this;
        }


        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public AffirmHttpResponse build() {
            return new AffirmHttpResponse(this);
        }
    }
}

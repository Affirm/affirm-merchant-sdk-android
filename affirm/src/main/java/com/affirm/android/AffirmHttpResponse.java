package com.affirm.android;

final class AffirmHttpResponse {
    private final int statusCode;
    private final String content;
    private final long totalSize;
    private final String contentType;

    private AffirmHttpResponse(Builder builder) {
        statusCode = builder.mStatusCode;
        content = builder.mContent;
        totalSize = builder.mTotalSize;
        contentType = builder.mContentType;
    }

    boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getContent() {
        return content;
    }

    long getTotalSize() {
        return totalSize;
    }

    String getContentType() {
        return contentType;
    }

    static final class Builder {

        private int mStatusCode;
        private String mContent;
        private long mTotalSize;
        private String mContentType;

        Builder() {
            mTotalSize = -1;
        }

        Builder(AffirmHttpResponse response) {
            super();
            setStatusCode(response.getStatusCode());
            setContent(response.getContent());
            setTotalSize(response.getTotalSize());
            setContentType(response.getContentType());
        }

        Builder setStatusCode(int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        Builder setContent(String content) {
            mContent = content;
            return this;
        }

        Builder setTotalSize(long totalSize) {
            mTotalSize = totalSize;
            return this;
        }


        Builder setContentType(String contentType) {
            mContentType = contentType;
            return this;
        }

        AffirmHttpResponse build() {
            return new AffirmHttpResponse(this);
        }
    }
}

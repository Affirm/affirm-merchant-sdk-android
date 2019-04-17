package com.affirm.android;

final class AffirmHttpResponse {
    private final int mStatusCode;
    private final String mContent;
    private final long mTotalSize;
    private final String mContentType;

    private AffirmHttpResponse(Builder builder) {
        mStatusCode = builder.mStatusCode;
        mContent = builder.mContent;
        mTotalSize = builder.mTotalSize;
        mContentType = builder.mContentType;
    }

    boolean isSuccessful() {
        return mStatusCode >= 200 && mStatusCode < 300;
    }

    int getStatusCode() {
        return mStatusCode;
    }

    String getContent() {
        return mContent;
    }

    long getTotalSize() {
        return mTotalSize;
    }

    String getContentType() {
        return mContentType;
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

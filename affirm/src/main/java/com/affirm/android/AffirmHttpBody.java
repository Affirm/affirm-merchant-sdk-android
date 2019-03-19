package com.affirm.android;

class AffirmHttpBody {

    private final String mContentType;
    private final String mContent;

    AffirmHttpBody(String contentType, String content) {
        mContentType = contentType;
        mContent = content;
    }

    String getContent() {
        return mContent;
    }

    String getContentType() {
        return mContentType;
    }
}

package com.affirm.android;

class AffirmHttpBody {

    private final String contentType;
    private final String content;

    AffirmHttpBody(String contentType, String content) {
        this.contentType = contentType;
        this.content = content;
    }

    String getContent() {
        return content;
    }

    String getContentType() {
        return contentType;
    }
}

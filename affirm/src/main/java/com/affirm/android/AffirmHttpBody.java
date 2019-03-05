package com.affirm.android;

class AffirmHttpBody {

    private final String contentType;
    private final String content;

    public AffirmHttpBody(String contentType, String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }
}

package com.affirm.android.http;

public class AffirmHttpBody {

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

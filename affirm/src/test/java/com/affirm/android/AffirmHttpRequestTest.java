package com.affirm.android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AffirmHttpRequestTest {

    @Test
    public void testHttpRequestMethod() {
        String url = "www.affirm.com";
        AffirmHttpRequest.Method method = AffirmHttpRequest.Method.POST;

        String content = "content";
        String contentType = "application/json";
        String tag = "tag";
        AffirmHttpBody body = new AffirmHttpBody(contentType, content);

        AffirmHttpRequest request = new AffirmHttpRequest.Builder()
                .setUrl(url)
                .setMethod(method)
                .setBody(body)
                .setTag(tag)
                .build();

        assertEquals(url, request.getUrl());
        assertEquals(method.toString(), request.getMethod().toString());
        AffirmHttpBody bodyAgain = request.getBody();
        assertEquals(contentType, bodyAgain.getContentType());
        assertEquals(content, bodyAgain.getContent());
    }
}

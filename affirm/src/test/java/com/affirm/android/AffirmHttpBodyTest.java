package com.affirm.android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AffirmHttpBodyTest {

    @Test
    public void testHttpRequestBody() {
        String content = "content";
        String contentType = "application/json";
        AffirmHttpBody body = new AffirmHttpBody(contentType, content);

        assertEquals(body.getContent(), content);
        assertEquals(body.getContentType(), contentType);
    }
}
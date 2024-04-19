package com.affirm.android.exception;

import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.AffirmError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InvalidRequestExceptionTest {

    private static final String affirmErrorJson = "{\"status_code\":400,\"type\":\"invalid-request\",\"code\":\"invalid_field\",\"message\":\"Bad request\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testWithOutThrowable() {
        AffirmError affirmError = gson.fromJson(affirmErrorJson, AffirmError.class);

        InvalidRequestException ex =  new InvalidRequestException(
                affirmError.message(),
                affirmError.type(),
                affirmError.fields(),
                affirmError.field(),
                "req_123",
                affirmError.status(),
                affirmError,
                null
        );
        assertEquals(
                "Bad request",
                ex.getMessage()
        );
    }
}

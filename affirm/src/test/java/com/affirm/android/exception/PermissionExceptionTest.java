package com.affirm.android.exception;

import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.AffirmError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PermissionExceptionTest {

    private static final String affirmErrorJson = "{\"status_code\":403,\"type\":\"forbidden\",\"code\":\"forbidden\",\"message\":\"Forbidden\"}";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create();

    @Test
    public void testWithOutThrowable() {
        AffirmError affirmError = gson.fromJson(affirmErrorJson, AffirmError.class);

        PermissionException ex =  new PermissionException(
                affirmError.message(),
                "req_123",
                affirmError.status(),
                affirmError
        );
        assertEquals(
                "Forbidden",
                ex.getMessage()
        );
    }
}

package com.affirm.android.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class APIExceptionTest {

    @Test
    public void testWithOutThrowable() {
        APIException ex = new APIException(
                "Error getting exception from response",
                null
        );
        assertEquals(
                "Error getting exception from response",
                ex.getMessage()
        );
    }

    @Test
    public void testWithThrowable() {
        APIException ex = new APIException(
                "Error getting exception from response",
                new Throwable("Something error")
        );
        assertEquals(
                "Error getting exception from response",
                ex.getMessage()
        );
    }
}

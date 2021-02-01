package com.affirm.android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AffirmLogTest {

    @Test
    public void logLevelTest() {
        int logLevel = Affirm.LOG_LEVEL_DEBUG;
        AffirmLog.setLogLevel(logLevel);
        assertEquals(logLevel, AffirmLog.getLogLevel());
    }
}

package com.affirm.android;

import android.util.Log;

import static com.affirm.android.Constants.SDK_NAME;

final class AffirmLog {

    private AffirmLog() {
    }

    private static final String TAG = SDK_NAME;

    private static int logLevel = Integer.MAX_VALUE;

    static int getLogLevel() {
        return logLevel;
    }

    static void setLogLevel(int logLevel) {
        AffirmLog.logLevel = logLevel;
    }

    static void v(String message, Throwable tr) {
        log(Log.VERBOSE, message, tr);
    }

    static void v(String message) {
        v(message, null);
    }

    static void d(String message, Throwable tr) {
        log(Log.DEBUG, message, tr);
    }

    static void d(String message) {
        d(message, null);
    }

    static void i(String message, Throwable tr) {
        log(Log.INFO, message, tr);
    }

    static void i(String message) {
        i(message, null);
    }

    static void w(String message, Throwable tr) {
        log(Log.WARN, message, tr);
    }

    static void w(String message) {
        w(message, null);
    }

    static void e(String message, Throwable tr) {
        log(Log.ERROR, message, tr);
    }

    static void e(String message) {
        e(message, null);
    }

    private static void log(int messageLogLevel, String message, Throwable tr) {
        if (messageLogLevel >= logLevel) {
            if (tr == null) {
                Log.println(messageLogLevel, TAG, message);
            } else {
                Log.println(messageLogLevel, TAG, message + '\n' + Log.getStackTraceString(tr));
            }
        }
    }
}

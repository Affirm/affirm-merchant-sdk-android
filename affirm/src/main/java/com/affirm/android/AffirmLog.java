package com.affirm.android;

import android.util.Log;

class AffirmLog {

    private static final String TAG = "Affirm";

    private static int logLevel = Integer.MAX_VALUE;

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(int logLevel) {
        AffirmLog.logLevel = logLevel;
    }

    private static void log(int messageLogLevel, String message, Throwable tr) {
        if (messageLogLevel >= logLevel) {
            if (tr == null) {
                Log.println(logLevel, TAG, message);
            } else {
                Log.println(logLevel, TAG, message + '\n' + Log.getStackTraceString(tr));
            }
        }
    }

    public static void v(String message, Throwable tr) {
        log(Log.VERBOSE, message, tr);
    }

    public static void v(String message) {
        v(message, null);
    }

    public static void d(String message, Throwable tr) {
        log(Log.DEBUG, message, tr);
    }

    public static void d(String message) {
        d(message, null);
    }

    public static void i(String message, Throwable tr) {
        log(Log.INFO, message, tr);
    }

    public static void i(String message) {
        i(message, null);
    }

    public static void w(String message, Throwable tr) {
        log(Log.WARN, message, tr);
    }

    public static void w(String message) {
        w(message, null);
    }

    public static void e(String message, Throwable tr) {
        log(Log.ERROR, message, tr);
    }

    public static void e(String message) {
        e(message, null);
    }
}

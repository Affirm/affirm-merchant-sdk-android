package com.affirm.android.log;

import android.util.Log;

public class AffirmLog {

    private static int logLevel = Integer.MAX_VALUE;

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(int logLevel) {
        AffirmLog.logLevel = logLevel;
    }

    private static void log(int messageLogLevel, String tag, String message, Throwable tr) {
        if (messageLogLevel >= logLevel) {
            if (tr == null) {
                Log.println(logLevel, tag, message);
            } else {
                Log.println(logLevel, tag, message + '\n' + Log.getStackTraceString(tr));
            }
        }
    }

    public static void v(String tag, String message, Throwable tr) {
        log(Log.VERBOSE, tag, message, tr);
    }

    public static void v(String tag, String message) {
        v(tag, message, null);
    }

    public static void d(String tag, String message, Throwable tr) {
        log(Log.DEBUG, tag, message, tr);
    }

    public static void d(String tag, String message) {
        d(tag, message, null);
    }

    public static void i(String tag, String message, Throwable tr) {
        log(Log.INFO, tag, message, tr);
    }

    public static void i(String tag, String message) {
        i(tag, message, null);
    }

    public static void w(String tag, String message, Throwable tr) {
        log(Log.WARN, tag, message, tr);
    }

    public static void w(String tag, String message) {
        w(tag, message, null);
    }

    public static void e(String tag, String message, Throwable tr) {
        log(Log.ERROR, tag, message, tr);
    }

    public static void e(String tag, String message) {
        e(tag, message, null);
    }
}

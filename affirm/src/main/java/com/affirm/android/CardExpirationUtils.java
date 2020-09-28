package com.affirm.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class CardExpirationUtils {

    private CardExpirationUtils() {
    }

    private static final String SP_NAME = "affirm_data";
    private static final String CACHED_CHECKOUT_ID = "cached_checkout_id";

    private static final String EXPIRATION_LAST_SAVED_TIME = "expiration_last_saved_time";
    private static final String EXPIRATION_REMAINING_TIME = "expiration_remaining_time";

    private static void saveLong(Context applicationContext, String key, long value) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong(key, value).apply();
    }

    private static void saveString(Context applicationContext, String key, String value) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, value).apply();
    }

    private static long getLong(Context applicationContext, String key) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.getLong(key, 0);
    }

    private static String getString(Context applicationContext, String key) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    private static void clearKey(Context applicationContext, String key) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key).apply();
    }

    private static boolean containsKey(Context applicationContext, String key) {
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences(SP_NAME,
                Context.MODE_PRIVATE);
        return sharedPreferences.contains(key);
    }

    public static void saveCachedCheckoutId(Context applicationContext, String checkoutId) {
        saveString(applicationContext, CACHED_CHECKOUT_ID, checkoutId);
        clearKey(applicationContext, EXPIRATION_LAST_SAVED_TIME);
        clearKey(applicationContext, EXPIRATION_REMAINING_TIME);
    }

    public static String getCachedCheckoutId(Context applicationContext) {
        return getString(applicationContext, CACHED_CHECKOUT_ID);
    }

    public static void clearCachedCheckoutId(Context applicationContext) {
        clearKey(applicationContext, CACHED_CHECKOUT_ID);
        clearKey(applicationContext, EXPIRATION_LAST_SAVED_TIME);
        clearKey(applicationContext, EXPIRATION_REMAINING_TIME);
    }

    public static void saveCardExpiredTime(Context applicationContext, long remaining,
                                           long timeMillis) {
        saveLong(applicationContext, EXPIRATION_REMAINING_TIME, remaining);
        saveLong(applicationContext, EXPIRATION_LAST_SAVED_TIME, timeMillis);
    }

    public static boolean isCardExpired(Context applicationContext) {
        return cardExpiredTimeMillis(applicationContext) < 0;
    }

    public static long cardExpiredTimeMillis(Context applicationContext) {
        if (containsKey(applicationContext, EXPIRATION_REMAINING_TIME)
                && containsKey(applicationContext, EXPIRATION_LAST_SAVED_TIME)) {
            long pastTime = System.currentTimeMillis()
                    - getLong(applicationContext, EXPIRATION_LAST_SAVED_TIME);
            return getLong(applicationContext, EXPIRATION_REMAINING_TIME) - pastTime;
        }
        return -1;
    }
}

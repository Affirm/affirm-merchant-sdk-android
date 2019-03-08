package com.affirm.android;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.util.Vector;

final class CookiesUtil {

    static void clearCookieByUrl(String url, CookieManager cookieManager,
                                 CookieSyncManager cookieSyncManager) {
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        clearCookieByUrlInternal(url, cookieManager, cookieSyncManager);
        clearCookieByUrlInternal("http://." + host, cookieManager, cookieSyncManager);
        clearCookieByUrlInternal("https://." + host, cookieManager, cookieSyncManager);
    }

    private static void clearCookieByUrlInternal(String url, CookieManager cookieManager,
                                                 CookieSyncManager cookieSyncManager) {

        String cookieString = cookieManager.getCookie(url);
        Vector<String> cookie = getCookieNamesByUrl(cookieString);
        if (cookie == null || cookie.isEmpty()) {
            return;
        }
        int len = cookie.size();
        for (int i = 0; i < len; i++) {
            cookieManager.setCookie(url, cookie.get(i) + "=-1");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cookieSyncManager.sync();
        } else {
            cookieManager.flush();
        }
    }

    private static Vector<String> getCookieNamesByUrl(String cookie) {
        if (TextUtils.isEmpty(cookie)) {
            return null;
        }
        String[] cookieField = cookie.split(";");
        int len = cookieField.length;
        for (int i = 0; i < len; i++) {
            cookieField[i] = cookieField[i].trim();
        }
        Vector<String> allCookieField = new Vector<>();
        for (String aCookieField : cookieField) {
            if (TextUtils.isEmpty(aCookieField)) {
                continue;
            }
            if (!aCookieField.contains("=")) {
                continue;
            }
            String[] singleCookieField = aCookieField.split("=");
            allCookieField.add(singleCookieField[0]);
        }
        if (allCookieField.isEmpty()) {
            return null;
        }
        return allCookieField;
    }
}

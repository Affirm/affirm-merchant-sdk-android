package com.affirm.android;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.CookieManager;

import java.util.Vector;

import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;

public final class CookiesUtil {
    static final String MAIN_DOMAIN = "https://.affirm.com/";

    private CookiesUtil() {
    }

    public static void clearCookies(Context context) {
        final CookieManager cookieManager = CookieManager.getInstance();
        clearCookieByUrlInternal(HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl() + "/", cookieManager);
    }


    private static void clearCookieByUrlInternal(String url, CookieManager cookieManager) {

        String urlCookieString = cookieManager.getCookie(url);
        String baseDomainCookieString = cookieManager.getCookie(MAIN_DOMAIN);
        Vector<String> urlCookies = getCookieNamesByUrl(urlCookieString);
        Vector<String> baseDomainCookies = getCookieNamesByUrl(baseDomainCookieString);

        urlCookies.removeAll(baseDomainCookies); //remove duplicates or we will be double writing cookies

        unsetCookies(MAIN_DOMAIN, baseDomainCookies, cookieManager);
        unsetCookies(url, urlCookies, cookieManager);

        cookieManager.flush();
    }

    private static void unsetCookies(String url, Vector<String> urlCookies, CookieManager cookieManager) {
        if (urlCookies == null) return;

        for (int i = 0; i < urlCookies.size(); i++) {
            cookieManager.setCookie(url, urlCookies.get(i) + "=");
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

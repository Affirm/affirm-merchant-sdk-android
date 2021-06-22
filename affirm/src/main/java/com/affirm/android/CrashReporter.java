package com.affirm.android;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrashReporter {

    private static final String AWS_PATH = "https://affirm-logs.s3.amazonaws.com/H5TW2EPRvm/";

    public static void upload(String url, String userAgent) {
        JSONObject jsonObject = new JSONObject();
        try {
            PackageInfo pInfo = getCurrentWebViewPackageInfo();
            if (pInfo != null) {
                jsonObject.put("WebView Version",
                        pInfo.packageName + ", " + pInfo.versionName);
            }
            jsonObject.put("WebView Url", url);
            jsonObject.put("WebView User Agent", userAgent);
            jsonObject.put("SDK Version", BuildConfig.VERSION_NAME);
            jsonObject.put("Locale", Locale.getDefault());
            jsonObject.put("Phone Model", Build.MODEL);
            jsonObject.put("Android Version", Build.VERSION.RELEASE);
            jsonObject.put("Board", Build.BOARD);
            jsonObject.put("Brand", Build.BRAND);
            jsonObject.put("Device", Build.DEVICE);
            jsonObject.put("Host", Build.HOST);
            jsonObject.put("ID", Build.ID);
            jsonObject.put("Product", Build.PRODUCT);
            jsonObject.put("Type", Build.TYPE);
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = RequestBody.create(jsonObject.toString().getBytes());
            Request request = new Request.Builder()
                    .url(AWS_PATH + UUID.randomUUID().toString())
                    .put(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    AffirmLog.e("Track crash failed", e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // https://stackoverflow.com/a/51735502/15137819
    @SuppressLint({"PrivateApi", "WebViewApiAvailability"})
    public static @Nullable
    PackageInfo getCurrentWebViewPackageInfo() {
        PackageInfo pInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //starting with Android O (API 26) they added a new method specific for this
            pInfo = WebView.getCurrentWebViewPackage();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //with Android Lollipop (API 21) they started to update the WebView
            //as a separate APK with the PlayStore and they added the
            //getLoadedPackageInfo() method to the WebViewFactory class and this
            //should handle the Android 7.0 behaviour changes too
            try {
                Method method = Class.forName("android.webkit.WebViewFactory")
                        .getMethod("getLoadedPackageInfo");
                pInfo = (PackageInfo) method.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            //before Lollipop the WebView was bundled with the
            //OS, the fixed versions can be found online, for example:
            //Android 4.4 has WebView version 30.0.0.0
            //Android 4.4.3 has WebView version 33.0.0.0
            //etc...
        }
        return pInfo;
    }
}

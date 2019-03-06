package com.affirm.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.Window;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public final class AffirmUtils {

    private static final String PLACEHOLDER_START = "{{";
    private static final String PLACEHOLDER_END = "}}";

    private AffirmUtils() {
    }

    public static int decimalDollarsToIntegerCents(float amount) {
        return (int) (amount * 100);
    }

    static String readInputStream(InputStream inputStream) throws IOException {
        final BufferedReader r =
                new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        final StringBuilder total = new StringBuilder();
        String line;

        while ((line = r.readLine()) != null) {
            total.append(line).append('\n');
        }

        return total.toString();
    }

    static String replacePlaceholders(@NonNull String text, @NonNull Map<String, String> map) {
        for (Object o : map.entrySet()) {
            Map.Entry pair = (Map.Entry) o;

            final String placeholder = PLACEHOLDER_START + pair.getKey() + PLACEHOLDER_END;
            text = text.replace(placeholder, (String) pair.getValue());
        }

        return text;
    }

    static void debuggableWebView(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
    }

    static void hideActionBar(AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (activity.getActionBar() != null) {
            activity.getActionBar().hide();
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }

    static void showCloseActionBar(AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (activity.getActionBar() != null) {
            activity.getActionBar().show();
            activity.getActionBar().setDisplayShowTitleEnabled(false);
            activity.getActionBar().setDisplayHomeAsUpEnabled(true);
//            activity.getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close);
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close);
        }
    }
}

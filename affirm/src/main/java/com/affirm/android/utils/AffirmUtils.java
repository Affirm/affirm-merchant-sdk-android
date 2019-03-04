package com.affirm.android.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;

import androidx.annotation.NonNull;

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
        final Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

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
}

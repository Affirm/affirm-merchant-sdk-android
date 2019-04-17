package com.affirm.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
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
import androidx.core.content.ContextCompat;

import static com.affirm.android.AffirmConstants.PLACEHOLDER_END;
import static com.affirm.android.AffirmConstants.PLACEHOLDER_START;

public final class AffirmUtils {

    private AffirmUtils() {
    }

    public static int decimalDollarsToIntegerCents(float amount) {
        return (int) (amount * 100);
    }

    static String readInputStream(@NonNull InputStream inputStream) throws IOException {
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

    static void hideActionBar(@NonNull AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        if (activity.getActionBar() != null) {
            activity.getActionBar().hide();
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().hide();
        }
    }

    static void showCloseActionBar(@NonNull AppCompatActivity activity) {
        activity.getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        Drawable drawable =
                ContextCompat.getDrawable(activity, R.drawable.affirm_ic_baseline_close);
        if (drawable != null) {
            drawable.setColorFilter(
                    ContextCompat.getColor(activity, R.color.affirm_ic_close_color),
                    PorterDuff.Mode.SRC_ATOP);
        }
        if (activity.getActionBar() != null) {
            activity.getActionBar().show();
            activity.getActionBar().setDisplayShowTitleEnabled(false);
            activity.getActionBar().setDisplayHomeAsUpEnabled(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                activity.getActionBar().setHomeAsUpIndicator(drawable);
            }
        } else if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().show();
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setHomeAsUpIndicator(drawable);
        }
    }

    static Activity getActivityFromView(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }
}

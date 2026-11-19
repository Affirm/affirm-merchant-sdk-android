package com.affirm.samples;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Shows the SDK's logcat lines (tag "Affirm") since the last {@link #mark}. An app can read its
 * own process's log without any permission.
 */
final class LogPanel {

    private static final String TAG = "AffirmSample";
    private static final String MARKER = "---- ";
    private static final long REFRESH_MS = 1000;

    private final TextView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            // The next read is scheduled when this one is done, so slow reads do not pile up.
            final int run = generation;
            reader.execute(() -> {
                final String text = withoutNoise(read());
                handler.post(() -> {
                    // Setting the same text again would lay out the page again.
                    if (!TextUtils.equals(text, view.getText())) {
                        view.setText(text);
                    }
                    // A read still running from before stop() must not start a second loop.
                    if (run == generation) {
                        handler.postDelayed(refresh, REFRESH_MS);
                    }
                });
            });
        }
    };
    private int generation;

    LogPanel(@NonNull TextView view) {
        this.view = view;
    }

    /** Starts a new section; the panel shows only the lines after the last mark. */
    static void mark(@NonNull String message) {
        Log.i(TAG, MARKER + message);
    }

    /** Adds a line to the current section. */
    static void note(@NonNull String message) {
        Log.i(TAG, message);
    }

    void start() {
        stop();
        handler.post(refresh);
    }

    void stop() {
        generation++;
        handler.removeCallbacks(refresh);
    }

    void release() {
        stop();
        reader.shutdownNow();
    }

    /** The SDK's log lines since the last {@link #mark}. */
    @WorkerThread
    @NonNull
    static String read() {
        final List<String> lines = new ArrayList<>();
        try {
            final Process process = new ProcessBuilder(
                    "logcat", "-d", "-v", "time", "-s", "Affirm:V", TAG + ":V")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("/" + TAG + "(") && line.contains("): " + MARKER)) {
                        lines.clear();
                    }
                    if (!line.startsWith("--------- beginning of")) {
                        lines.add(line);
                    }
                }
            }
        } catch (IOException e) {
            return "Could not read logcat: " + e.getMessage();
        }
        return TextUtils.join("\n", lines);
    }

    /** Drops the promo pages' console messages and stack frames, which crowd the screen. */
    @NonNull
    private static String withoutNoise(@NonNull String log) {
        final List<String> lines = new ArrayList<>();
        for (String line : log.split("\n")) {
            if (!line.contains(" -- From line ") && !line.contains("): \t")) {
                lines.add(line);
            }
        }
        return TextUtils.join("\n", lines);
    }
}

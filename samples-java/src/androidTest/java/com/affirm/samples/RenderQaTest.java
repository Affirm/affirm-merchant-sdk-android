package com.affirm.samples;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.Until;

import com.affirm.android.AffirmPromotionButton;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Runs the promo render QA matrix on a device against real responses, and saves a screenshot,
 * the SDK log and a summary.md for each step. Skipped unless {@code qaCases} is passed; run it
 * with {@code ./gradlew :samples-java:renderQa}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RenderQaTest {

    private static final String HTML = "HTML";
    private static final String NATIVE = "Native";
    private static final long SETTLE_TIMEOUT_MS = 30_000;
    // A cached message is shown while the request runs, so a state must hold for a while.
    private static final long STABLE_MS = 4_000;
    private static final long POLL_MS = 250;
    private static final long MODAL_TIMEOUT_MS = 15_000;
    private static final int STATUS_PROGRESS = 2;

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final UiDevice device = UiDevice.getInstance(instrumentation);
    private final List<Step> steps = new ArrayList<>();
    private File outputDir;

    @Test
    public void runRenderQaMatrix() throws IOException {
        final Bundle args = InstrumentationRegistry.getArguments();
        final String cases = args.getString("qaCases");
        assumeTrue("Pass qaCases to run the render QA", cases != null && !cases.trim().isEmpty());
        final String runId = args.getString("runId", String.valueOf(System.currentTimeMillis()));
        outputDir = new File(instrumentation.getTargetContext().getExternalFilesDir("affirm-qa"),
                runId);
        assertTrue("Could not create " + outputDir, outputDir.mkdirs() || outputDir.isDirectory());

        final RenderMigrationActivity demo = launch(RenderMigrationActivity.class);
        for (String spec : cases.split(",")) {
            runCase(demo, QaCase.parse(spec));
        }
        onMain(demo::finish);

        writeSummary(runId);
        final StringBuilder failures = new StringBuilder();
        for (Step step : steps) {
            if (!step.passed) {
                failures.append("\n").append(step.line());
            }
        }
        assertTrue("Render QA failed:" + failures + "\nSee " + outputDir,
                failures.length() == 0);
    }

    private void runCase(RenderMigrationActivity demo, QaCase qaCase) {
        final AffirmPromotionButton promo = demo.findViewById(R.id.native_api_promo);
        final RadioGroup font = demo.findViewById(R.id.font);
        final SwitchCompat optOut = demo.findViewById(R.id.opt_out);
        onMain(() -> {
            font.check(R.id.font_default);
            optOut.setChecked(false);
            ((RadioGroup) demo.findViewById(R.id.country)).check(qaCase.countryButton());
            ((EditText) demo.findViewById(R.id.public_key)).setText(qaCase.publicKey);
            ((EditText) demo.findViewById(R.id.amount)).setText(qaCase.amount);
            demo.findViewById(R.id.load).performClick();
        });
        checkRenderer(demo, qaCase, "System font, useLegacyNativeRendering=false",
                qaCase.expected, promo);

        onMain(() -> optOut.setChecked(true));
        checkRenderer(demo, qaCase, "System font, useLegacyNativeRendering=true", NATIVE, promo);
        onMain(() -> optOut.setChecked(false));

        onMain(() -> font.check(R.id.font_custom));
        checkRenderer(demo, qaCase, "Custom font (Lacquer), useLegacyNativeRendering=false",
                NATIVE, promo);
        onMain(() -> optOut.setChecked(true));
        checkRenderer(demo, qaCase, "Custom font (Lacquer), useLegacyNativeRendering=true",
                NATIVE, promo);
        onMain(() -> {
            optOut.setChecked(false);
            font.check(R.id.font_default);
        });
        waitSettled(promo);

        tapPromo(demo, qaCase, promo);
        scrollList(demo, qaCase);
    }

    private void checkRenderer(RenderMigrationActivity demo, QaCase qaCase, String scenario,
                               String expected, AffirmPromotionButton promo) {
        final String actual = waitSettled(promo);
        showPromos(demo);
        record(qaCase, scenario, expected, actual, expected.equals(actual));
    }

    private void tapPromo(RenderMigrationActivity demo, QaCase qaCase,
                          AffirmPromotionButton promo) {
        showPromos(demo);
        final AtomicReference<String> shownAs = new AtomicReference<>();
        final int[] center = new int[2];
        onMain(() -> {
            shownAs.set(PromoViewState.shownAs(promo));
            promo.getLocationOnScreen(center);
            center[0] += promo.getWidth() / 2;
            center[1] += promo.getHeight() / 2;
        });
        device.click(center[0], center[1]);
        final Activity opened = waitForResumed(
                activity -> activity.getClass().getName().startsWith("com.affirm.android."),
                MODAL_TIMEOUT_MS);
        if (opened != null) {
            // Let the modal load its page before the screenshot.
            device.wait(Until.hasObject(By.pkg(opened.getPackageName())
                    .text(Pattern.compile("(?is).*affirm.*"))), MODAL_TIMEOUT_MS);
            SystemClock.sleep(1_000);
        }
        record(qaCase, "Tap the promo shown as " + shownAs.get(), "Affirm modal opens",
                opened == null ? "nothing opened" : opened.getClass().getSimpleName() + " opened",
                opened != null);
        for (int i = 0; i < 3 && waitForResumed(demo.getClass()::isInstance, 2_000) == null; i++) {
            device.pressBack();
        }
    }

    private void scrollList(RenderMigrationActivity demo, QaCase qaCase) {
        LogPanel.mark("PromoListActivity · " + qaCase);
        final PromoListActivity list = launch(PromoListActivity.class);
        final RecyclerView rows = list.findViewById(R.id.promo_list);
        for (int direction : new int[]{1, -1}) {
            for (int i = 0; i < 8; i++) {
                onMain(() -> rows.scrollBy(0, direction * rows.getHeight() / 2));
                SystemClock.sleep(300);
            }
        }
        final Map<String, Integer> counts = waitRowsSettled(rows);
        boolean passed = !counts.isEmpty();
        for (String state : counts.keySet()) {
            passed &= state.equals(qaCase.expected) || state.equals("hidden");
        }
        record(qaCase, "Scroll 100 rows down and back up", "visible rows " + qaCase.expected,
                "visible rows " + counts, passed);
        onMain(list::finish);
        waitForResumed(demo.getClass()::isInstance, 5_000);
    }

    /** What {@code promo} shows once it stops changing, or why it did not settle. */
    private String waitSettled(AffirmPromotionButton promo) {
        final long deadline = SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MS;
        String last = null;
        long since = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() < deadline) {
            final AtomicReference<String> state = new AtomicReference<>();
            onMain(() -> state.set(PromoViewState.shownAs(promo)));
            if (!state.get().equals(last)) {
                last = state.get();
                since = SystemClock.uptimeMillis();
            } else if (!last.startsWith("loading")
                    && SystemClock.uptimeMillis() - since >= STABLE_MS) {
                return last;
            }
            SystemClock.sleep(POLL_MS);
        }
        return "not settled after " + SETTLE_TIMEOUT_MS / 1000 + " s (" + last + ")";
    }

    /** How many visible rows show each state, once none is loading and nothing changes. */
    private Map<String, Integer> waitRowsSettled(RecyclerView rows) {
        final long deadline = SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MS;
        Map<String, Integer> last = null;
        long since = SystemClock.uptimeMillis();
        while (SystemClock.uptimeMillis() < deadline) {
            final Map<String, Integer> counts = new TreeMap<>();
            onMain(() -> {
                for (int i = 0; i < rows.getChildCount(); i++) {
                    final AffirmPromotionButton promo =
                            rows.getChildAt(i).findViewById(R.id.item_promo);
                    final String state = PromoViewState.shownAs(promo);
                    final Integer count = counts.get(state);
                    counts.put(state, count == null ? 1 : count + 1);
                }
            });
            boolean loading = false;
            for (String state : counts.keySet()) {
                loading |= state.startsWith("loading");
            }
            if (!counts.equals(last)) {
                last = counts;
                since = SystemClock.uptimeMillis();
            } else if (!loading && SystemClock.uptimeMillis() - since >= STABLE_MS) {
                return counts;
            }
            SystemClock.sleep(POLL_MS);
        }
        return last == null ? new TreeMap<>() : last;
    }

    /** Scrolls the demo so the opt-out switch, the fonts and the promo are on screen. */
    private void showPromos(RenderMigrationActivity demo) {
        onMain(() -> {
            final View optOut = demo.findViewById(R.id.opt_out);
            final ScrollView scroll = (ScrollView) ((ViewGroup) demo.findViewById(
                    android.R.id.content)).getChildAt(0);
            scroll.scrollTo(0, Math.max(0, optOut.getTop() - optOut.getHeight()));
        });
        instrumentation.waitForIdleSync();
        SystemClock.sleep(500);
    }

    private void record(QaCase qaCase, String scenario, String expected, String actual,
                        boolean passed) {
        final Step step = new Step(steps.size() + 1, qaCase, scenario, expected, actual, passed);
        steps.add(step);
        device.takeScreenshot(new File(outputDir, step.fileName() + ".png"));
        step.log = LogPanel.read();
        try {
            write(new File(outputDir, step.fileName() + ".log"), step.log);
        } catch (IOException e) {
            step.log = "Could not save the log: " + e;
        }
        final Bundle status = new Bundle();
        status.putString("qa", step.line());
        instrumentation.sendStatus(STATUS_PROGRESS, status);
    }

    private void writeSummary(String runId) throws IOException {
        int passed = 0;
        for (Step step : steps) {
            passed += step.passed ? 1 : 0;
        }
        final StringBuilder md = new StringBuilder()
                .append("# Promo render QA\n\n")
                .append("- Run: ").append(runId).append('\n')
                .append("- Environment: ").append(BuildConfig.AFFIRM_ENVIRONMENT).append('\n')
                .append("- Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(", Android ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
                .append("- WebView: ").append(webViewVersion()).append('\n')
                .append("- Result: ").append(passed).append(" passed, ")
                .append(steps.size() - passed).append(" failed\n\n")
                .append("Expected and Actual are what the `configWithLocalStyling` promo shows.\n\n")
                .append("| # | Public key | Country | Amount | Scenario | Expected | Actual | Result |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        for (Step step : steps) {
            md.append("| ").append(step.number)
                    .append(" | `").append(step.qaCase.publicKey)
                    .append("` | ").append(step.qaCase.countryCode)
                    .append(" | ").append(step.qaCase.amount)
                    .append(" | ").append(step.scenario)
                    .append(" | ").append(step.expected)
                    .append(" | ").append(step.actual)
                    .append(" | ").append(step.passed ? "PASS" : "**FAIL**")
                    .append(" |\n");
        }
        for (Step step : steps) {
            md.append("\n## ").append(step.number).append(". ").append(step.qaCase)
                    .append(" · ").append(step.scenario).append("\n\n")
                    .append("Expected ").append(step.expected).append(", got ")
                    .append(step.actual).append(": ")
                    .append(step.passed ? "PASS" : "**FAIL**").append("\n\n")
                    .append("![").append(step.fileName()).append("](")
                    .append(step.fileName()).append(".png)\n\n")
                    .append("SDK log (render lines; full log in [").append(step.fileName())
                    .append(".log](").append(step.fileName()).append(".log)):\n\n```\n")
                    .append(renderLines(step.log)).append("\n```\n");
        }
        write(new File(outputDir, "summary.md"), md.toString());
    }

    /** The log lines that say which renderer was used and how the HTML went. */
    private static String renderLines(String log) {
        final StringBuilder lines = new StringBuilder();
        for (String line : log.split("\n")) {
            if (line.contains("): ---- ") || line.contains("): Promo ")
                    || line.contains("): Ignoring ")) {
                lines.append(line).append('\n');
            }
        }
        return lines.length() == 0 ? "(none)" : lines.toString().trim();
    }

    private static String webViewVersion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "unknown";
        }
        final PackageInfo webView = WebView.getCurrentWebViewPackage();
        return webView == null ? "none" : webView.packageName + " " + webView.versionName;
    }

    private static void write(File file, String text) throws IOException {
        try (Writer out = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            out.write(text);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Activity> T launch(Class<T> activityClass) {
        final Intent intent = new Intent(instrumentation.getTargetContext(), activityClass)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final T activity = (T) instrumentation.startActivitySync(intent);
        instrumentation.waitForIdleSync();
        return activity;
    }

    private Activity waitForResumed(Matcher matcher, long timeoutMs) {
        final long deadline = SystemClock.uptimeMillis() + timeoutMs;
        while (SystemClock.uptimeMillis() < deadline) {
            final AtomicReference<Activity> found = new AtomicReference<>();
            onMain(() -> {
                final Collection<Activity> resumed = ActivityLifecycleMonitorRegistry
                        .getInstance().getActivitiesInStage(Stage.RESUMED);
                for (Activity activity : resumed) {
                    if (matcher.matches(activity)) {
                        found.set(activity);
                    }
                }
            });
            if (found.get() != null) {
                return found.get();
            }
            SystemClock.sleep(POLL_MS);
        }
        return null;
    }

    private void onMain(Runnable action) {
        instrumentation.runOnMainSync(action);
    }

    private interface Matcher {
        boolean matches(Activity activity);
    }

    /** One {@code publicKey:amount:html|native[:USA|GBR|CAN]} entry of {@code qaCases}. */
    private static final class QaCase {
        final String publicKey;
        final String amount;
        final String expected;
        final String countryCode;

        private QaCase(String publicKey, String amount, String expected, String countryCode) {
            this.publicKey = publicKey;
            this.amount = amount;
            this.expected = expected;
            this.countryCode = countryCode;
        }

        static QaCase parse(String spec) {
            final String[] parts = spec.trim().split(":");
            if (parts.length != 3 && parts.length != 4) {
                throw new IllegalArgumentException("qaCases entry must be "
                        + "publicKey:amount:html|native[:USA|GBR|CAN], not " + spec);
            }
            final String expected = parts[2].trim().toLowerCase(Locale.US);
            if (!expected.equals("html") && !expected.equals("native")) {
                throw new IllegalArgumentException(
                        "Expected renderer must be html or native, not " + parts[2]);
            }
            final String countryCode =
                    parts.length == 4 ? parts[3].trim().toUpperCase(Locale.US) : "USA";
            if (!countryCode.equals("USA") && !countryCode.equals("GBR")
                    && !countryCode.equals("CAN")) {
                throw new IllegalArgumentException(
                        "Country must be USA, GBR or CAN, not " + parts[3]);
            }
            return new QaCase(parts[0].trim(), parts[1].trim(),
                    expected.equals("html") ? HTML : NATIVE, countryCode);
        }

        int countryButton() {
            switch (countryCode) {
                case "GBR":
                    return R.id.country_gbr;
                case "CAN":
                    return R.id.country_can;
                default:
                    return R.id.country_usa;
            }
        }

        @Override
        public String toString() {
            return publicKey + " · " + countryCode + " · " + amount;
        }
    }

    private static final class Step {
        final int number;
        final QaCase qaCase;
        final String scenario;
        final String expected;
        final String actual;
        final boolean passed;
        String log = "";

        Step(int number, QaCase qaCase, String scenario, String expected, String actual,
             boolean passed) {
            this.number = number;
            this.qaCase = qaCase;
            this.scenario = scenario;
            this.expected = expected;
            this.actual = actual;
            this.passed = passed;
        }

        String fileName() {
            final String slug = scenario.toLowerCase(Locale.US)
                    .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            return String.format(Locale.US, "%02d-%s-%s-%s", number, qaCase.publicKey,
                    qaCase.amount, slug);
        }

        String line() {
            return (passed ? "PASS " : "FAIL ") + number + ". " + qaCase + " · " + scenario
                    + ": expected " + expected + ", got " + actual;
        }
    }
}

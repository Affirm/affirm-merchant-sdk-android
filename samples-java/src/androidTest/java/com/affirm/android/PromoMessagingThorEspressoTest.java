package com.affirm.android;

import android.os.Bundle;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.affirm.samples.MainActivity;
import com.affirm.samples.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.lang.reflect.Field;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Real SDK/sample-app rendering against live Thor; no canned responses. */
@RunWith(AndroidJUnit4.class)
public class PromoMessagingThorEspressoTest {
    @Rule public ActivityTestRule<MainActivity> activity =
            new ActivityTestRule<>(MainActivity.class, true, false);
    private Field sdkClientField;
    private AffirmHttpClient originalClient;
    private String expected;

    @Before public void pointPromoRequestsAtThor() throws Exception {
        Bundle inputs = InstrumentationRegistry.getArguments();
        HttpUrl thor = HttpUrl.parse(required(inputs, "affirmPromoBaseUrl"));
        assertNotNull("Thor URL must be valid", thor);
        assertTrue("Expected HTTPS Thor host", thor.isHttps() && thor.host().endsWith(".affirm-thor.com"));
        expected = required(inputs, "affirmExpectedPromoText");
        Affirm.setPublicKey(required(inputs, "affirmPublicKey"));
        Affirm.setCountryCode("USA");
        Affirm.setLocale("en_US");

        // Test-only transport routing preserves SDK request construction and
        // response handling, without adding a public SDK API for test hosts.
        originalClient = AffirmPlugins.get().restClient();
        Field transport = AffirmHttpClient.class.getDeclaredField("okHttpClient");
        transport.setAccessible(true);
        OkHttpClient base = (OkHttpClient) transport.get(originalClient);
        OkHttpClient.Builder client = base.newBuilder().addInterceptor(chain -> {
            Request request = chain.request();
            if (request.url().encodedPath().startsWith("/api/promos/v2/")) {
                request = request.newBuilder().url(request.url().newBuilder()
                        .scheme(thor.scheme()).host(thor.host()).port(thor.port()).build()).build();
            }
            return chain.proceed(request);
        });
        sdkClientField = AffirmPlugins.class.getDeclaredField("restClient");
        sdkClientField.setAccessible(true);
        sdkClientField.set(AffirmPlugins.get(), AffirmHttpClient.createClient(client));
    }

    @After public void saveScreenshotAndRestoreClient() throws Exception {
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir("upfunnel-promo-screenshots");
        if (directory != null && (directory.exists() || directory.mkdirs())) {
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                    .takeScreenshot(new File(directory, "live-thor-promo.png"));
        }
        activity.finishActivity();
        if (sdkClientField != null) sdkClientField.set(AffirmPlugins.get(), originalClient);
    }

    @Test public void rendersPromoMessagingFromThorService() {
        activity.launchActivity(null);
        long deadline = SystemClock.uptimeMillis() + 30000;
        AssertionError last = null;
        do {
            try {
                onView(withId(R.id.promo)).check(matches(
                        hasDescendant(withContentDescription(containsString(expected)))));
                onView(withId(R.id.promotionTextView))
                        .check(matches(isDisplayed()))
                        .check(matches(withText(containsString(expected))))
                        .check(matches(withContentDescription(containsString(expected))));
                return;
            } catch (AssertionError error) {
                last = error;
                SystemClock.sleep(100);
            }
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }

    private String required(Bundle inputs, String name) {
        String value = inputs.getString(name);
        assertTrue("Missing live test input: " + name, value != null && !value.trim().isEmpty());
        return value;
    }
}

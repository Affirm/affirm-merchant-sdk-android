package com.affirm.android;

import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import com.affirm.samples.MainActivity;
import com.affirm.samples.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertTrue;

/** Real sample-app UI and SDK parsing/rendering, with only the HTTP response controlled. */
@RunWith(AndroidJUnit4.class)
public class PromoMessagingEmulatorTest {
    private static final String COPY = "Pay in four installments. Learn more";
    private static final String SUCCESS = "{\"promo\":{\"ala\":\"" + COPY
            + "\",\"html_ala\":\"<span>" + COPY
            + "</span>\",\"config\":{\"promo_prequal_enabled\":false}}}";

    @Rule public ActivityTestRule<MainActivity> activity =
            new ActivityTestRule<>(MainActivity.class, true, false);

    private final AtomicInteger promoRequests = new AtomicInteger();
    private volatile String body = SUCCESS;
    private volatile int status = 200;
    private Field clientField;
    private Object originalClient;

    @Before public void installControlledHttpResponses() throws Exception {
        // The SDK has no public network-client injection API. Reflection here is
        // confined to instrumentation and does not change the shipped library.
        clientField = AffirmPlugins.class.getDeclaredField("restClient");
        clientField.setAccessible(true);
        originalClient = clientField.get(AffirmPlugins.get());
        OkHttpClient.Builder client = new OkHttpClient.Builder().addInterceptor(chain -> {
            boolean promo = chain.request().url().encodedPath().startsWith("/api/promos/v2/");
            if (promo) {
                promoRequests.incrementAndGet();
            }
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(promo ? status : 200).message("Controlled emulator response")
                    .body(ResponseBody.create(MediaType.parse("application/json"), promo ? body : "{}"))
                    .build();
        });
        clientField.set(AffirmPlugins.get(), AffirmHttpClient.createClient(client));
    }

    @After public void restoreSdkClient() throws Exception {
        activity.finishActivity();
        clientField.set(AffirmPlugins.get(), originalClient);
    }

    @Test public void rendersNativePromoThroughRealSdk() {
        activity.launchActivity(null);
        eventually(() -> onView(withId(R.id.promo)).check(matches(
                hasDescendant(withContentDescription(containsString(COPY))))));
        assertTrue("The SDK must issue a real promo request", promoRequests.get() > 0);
    }

    @Test public void hidesNativePromoForEmptyMessaging() {
        body = "{\"promo\":{\"ala\":\"\",\"html_ala\":\"\",\"config\":{\"promo_prequal_enabled\":false}}}";
        assertPromoHidden();
    }

    @Test public void handlesApiErrorWithoutCrashingSampleApp() {
        status = 400;
        body = "{\"message\":\"Unavailable\",\"type\":\"invalid_request\",\"status_code\":400}";
        assertPromoHidden();
    }

    private void assertPromoHidden() {
        activity.launchActivity(null);
        eventually(() -> onView(withId(R.id.promo)).check(matches(
                withEffectiveVisibility(ViewMatchers.Visibility.GONE))));
        assertTrue("The SDK must issue a real promo request", promoRequests.get() > 0);
    }

    private void eventually(Runnable assertion) {
        long deadline = SystemClock.uptimeMillis() + 15000;
        AssertionError last = null;
        do {
            try { assertion.run(); return; }
            catch (AssertionError error) { last = error; SystemClock.sleep(100); }
        } while (SystemClock.uptimeMillis() < deadline);
        throw last;
    }
}

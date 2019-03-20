package com.affirm.samples;

import android.os.SystemClock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testPromo() {
        SystemClock.sleep(5000);
        onView(withId(R.id.promo)).check(matches(withText(startsWith("As low as"))));
    }

    @Test
    public void testPrequal() {
        SystemClock.sleep(5000);

        onView(withId(R.id.promo)).perform(click());

        onWebView().withTimeout(20, TimeUnit.SECONDS)
                .withElement(findElement(Locator.ID, "prequal-header"))
                .check(webMatches(getText(), containsString("Make easy monthly payments over 3, 6, or 12 months")));

        SystemClock.sleep(2000);

        pressBack();
    }
}

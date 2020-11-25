package com.affirm.samples;

import android.os.SystemClock;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.test.espresso.web.webdriver.DriverAtoms;
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
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testPromoModal() {
        SystemClock.sleep(1000);

        onView(withId(R.id.productModalButton)).perform(click());

        onWebView().withTimeout(20, TimeUnit.SECONDS);

        SystemClock.sleep(8000);

        pressBack();

        onView(withId(R.id.siteModalButton)).perform(click());

        onWebView().withTimeout(20, TimeUnit.SECONDS);

        SystemClock.sleep(8000);

        pressBack();
    }


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
                .check(webMatches(getText(), containsString("Pay over time")));

        SystemClock.sleep(2000);

        pressBack();
    }

    @Test
    public void testCheckout() {
        SystemClock.sleep(1000);

        onView(withId(R.id.checkout)).perform(click());

        onWebView().withTimeout(20, TimeUnit.SECONDS);

        SystemClock.sleep(2000);

        onWebView().withElement(findElement(Locator.ID, "input-phoneNumber"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys("8888888888"))
                .withElement(findElement(Locator.ID, "account-submit"))
                .perform(webClick());

        SystemClock.sleep(2000);

        pressBack();
    }
}

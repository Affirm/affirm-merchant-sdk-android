package com.affirm.samples;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.selectFrameByIdOrName;
import static org.hamcrest.CoreMatchers.startsWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityEspressoTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    public static Matcher<View> firstChildOf(final Matcher<View> parentMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with first child view of type parentMatcher");
            }

            @Override
            public boolean matchesSafely(View view) {

                if (!(view.getParent() instanceof ViewGroup)) {
                    return parentMatcher.matches(view.getParent());
                }
                ViewGroup group = (ViewGroup) view.getParent();
                return parentMatcher.matches(view.getParent()) && group.getChildAt(0).equals(view);

            }
        };
    }

    @Test
    public void testPromo() {
        SystemClock.sleep(8000);

        onView(withId(R.id.promotionTextView))
                .check(matches(isEnabled()))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))
                .check(matches(withText(startsWith("As low as"))));

        onWebView(withId(R.id.htmlPromotionWebView))
                .withElement(findElement(Locator.CLASS_NAME, "affirm-modal-trigger"))
                .check(webMatches(getText(), startsWith("As low as")));

        onView(firstChildOf(withId(R.id.promo)))
                .check(matches(isEnabled()))
                .check(matches(isDisplayed()))
                .check(matches(withText(startsWith("As low as"))));

        onWebView(firstChildOf(firstChildOf(withId(R.id.promo_container))))
                .withElement(findElement(Locator.CLASS_NAME, "affirm-modal-trigger"))
                .check(webMatches(getText(), startsWith("As low as")));
    }

    @Test
    public void testPrequal() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.clearCookies)).perform(click());
        SystemClock.sleep(8000);
        onView(withId(R.id.promo)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // See if you qualify
        UiObject appItem = device.findObject(new UiSelector().className("android.widget.ListView").instance(0)
                .childSelector(new UiSelector().className(View.class).instance(0))
        );

        appItem.waitForExists(timeOut);
        appItem.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));

        appView.scrollIntoView(new UiSelector().className(Button.class).instance(13));

        device.findObject(new UiSelector().text("See if you qualify").className(Button.class)).clickAndWaitForNewWindow();

        // Input number
        UiObject emailInput = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(timeOut);
        emailInput.setText("8888888888");

        appView.scrollIntoView(new UiSelector().instance(2).className(Button.class));
        UiObject buttonContinue = device.findObject(new UiSelector().text("Continue and open modal").className(Button.class));

        buttonContinue.waitForExists(timeOut);
        buttonContinue.clickAndWaitForNewWindow();

        // Input code
        UiObject inputValidate = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        inputValidate.waitForExists(timeOut);
        inputValidate.setText("1234");

        SystemClock.sleep(6000);
    }

    @Test
    public void testPrequalCancel() {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.clearCookies)).perform(click());
        SystemClock.sleep(8000);
        onView(withId(R.id.promo)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        UiObject buttonClose = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));

        buttonClose.waitForExists(timeOut);

        SystemClock.sleep(2000);

        onView(withContentDescription("Navigate up")).perform(click());

        SystemClock.sleep(2000);
    }

    @Test
    public void testCheckout() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.clearCookies)).perform(click());
        onView(withId(R.id.checkout)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // Input number
        UiObject emailInput = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(timeOut);
        emailInput.setText("8888888888");

        UiObject buttonContinue = device.findObject(new UiSelector()
                .instance(2)
                .className(Button.class));

        buttonContinue.waitForExists(timeOut);
        buttonContinue.clickAndWaitForNewWindow();

        // Input code
        UiObject inputValidate = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        inputValidate.waitForExists(timeOut);
        inputValidate.setText("1234");

        SystemClock.sleep(2000);

        // Select payment plan
        UiObject appItem = device.findObject(new UiSelector().className("android.widget.ListView").instance(0)
                .childSelector(new UiSelector().className(View.class).instance(0))
        );

        appItem.waitForExists(timeOut);
        appItem.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        // Review plan
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));

        appView.scrollToEnd(1);

        device.findObject(new UiSelector().text("Continue").className(Button.class)).clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        // Confirm
        appView.scrollIntoView(new UiSelector().className(Button.class).instance(5));

        UiObject checkbox = device.findObject(new UiSelector().className(CheckBox.class).instance(0));

        int w = device.getDisplayWidth();
        device.click(w / 2, checkbox.getVisibleBounds().centerY() + 30);

//        checkbox.waitForExists(timeOut);
//        checkbox.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        device.findObject(new UiSelector().resourceId("confirm-submit").className(Button.class)).clickAndWaitForNewWindow();

        SystemClock.sleep(10000);
    }

    @Test
    public void testCheckoutCancel() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.checkout)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        UiObject buttonClose = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));

        buttonClose.waitForExists(timeOut);
        buttonClose.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        UiObject buttonOK = device.findObject(new UiSelector()
                .text("OK")
                .className(Button.class));

        buttonOK.waitForExists(timeOut);
        buttonOK.clickAndWaitForNewWindow();
    }

    @Test
    public void testVCNCheckout() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.clearCookies)).perform(click());
        onView(withId(R.id.vcnCheckout)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // Input number
        UiObject emailInput = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(timeOut);
        emailInput.setText("8888888888");

        UiObject buttonContinue = device.findObject(new UiSelector()
                .instance(2)
                .className(Button.class));

        buttonContinue.waitForExists(timeOut);
        buttonContinue.clickAndWaitForNewWindow();

        // Input code
        UiObject inputValidate = device.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        inputValidate.waitForExists(timeOut);
        inputValidate.setText("1234");

        SystemClock.sleep(2000);

        // Select payment plan
        UiObject appItem = device.findObject(new UiSelector().className("android.widget.ListView").instance(0)
                .childSelector(new UiSelector().className(View.class).instance(0))
        );

        appItem.waitForExists(timeOut);
        appItem.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        // Review plan
        UiScrollable appView = new UiScrollable(new UiSelector().scrollable(true));

        appView.scrollToEnd(1);

        SystemClock.sleep(2000);

        int w = device.getDisplayWidth();
        int h = device.getDisplayHeight();
        device.click(w / 2, h - 400);

        // The uiautomator map doesn't have the right coordinates after scroll to bottom
//        UiObject continueButton = device.findObject(new UiSelector().text("Continue").className(Button.class));
//
//        continueButton.waitForExists(timeOut);
//        continueButton.clickAndWaitForNewWindow();

        SystemClock.sleep(1000);

        // Confirm
        UiObject checkbox = device.findObject(new UiSelector().className(CheckBox.class).instance(0));

        checkbox.waitForExists(timeOut);
        checkbox.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().className(Button.class).instance(5));

        // The uiautomator map doesn't have the right coordinates after scroll to bottom
//        device.findObject(new UiSelector().resourceId("confirm-submit").className(Button.class)).clickAndWaitForNewWindow();

        device.click(w / 2, h - 400);

        SystemClock.sleep(10000);
    }


    @Test
    public void testVCNCheckoutCancel() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.vcnCheckout)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        UiObject buttonClose = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));

        buttonClose.waitForExists(timeOut);
        buttonClose.clickAndWaitForNewWindow();

        SystemClock.sleep(2000);

        UiObject buttonOK = device.findObject(new UiSelector()
                .text("OK")
                .className(Button.class));

        buttonOK.waitForExists(timeOut);
        buttonOK.clickAndWaitForNewWindow();
    }

    @Test
    public void testSiteModal() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.siteModalButton)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        UiObject buttonClose = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));

        buttonClose.waitForExists(timeOut);
        buttonClose.click();

        SystemClock.sleep(2000);

        onView(withId(R.id.siteModalButton)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        SystemClock.sleep(2000);

        onWebView()
                .inWindow(selectFrameByIdOrName("affirm_learn_more_splitpay"))
                .withElement(findElement(Locator.CLASS_NAME, "affirm-tagline"))
                .check(webMatches(getText(), Matchers.containsString("Make easy monthly payments over 3, 6, or 12 months")))
                .withElement(findElement(Locator.CLASS_NAME, "affirm-promo-text"))
                .check(webMatches(getText(), Matchers.containsString("Rates from 10–30% APR.")))
                .check(webMatches(getCurrentUrl(), Matchers.containsString("https://sandbox.affirm.com/")));

        pressBack();
    }

    @Test
    public void testProductModal() throws Exception {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        onView(withId(R.id.productModalButton)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        UiObject buttonClose = device.findObject(new UiSelector()
                .instance(0)
                .className(Button.class));

        buttonClose.waitForExists(timeOut);
        buttonClose.click();

        SystemClock.sleep(2000);

        onView(withId(R.id.productModalButton)).perform(click());

        device.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        SystemClock.sleep(2000);

        onWebView()
                .check(webMatches(getCurrentUrl(), Matchers.containsString("https://sandbox.affirm.com/")));

        pressBack();
    }
}

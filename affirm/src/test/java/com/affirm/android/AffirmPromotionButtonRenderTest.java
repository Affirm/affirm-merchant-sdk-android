package com.affirm.android;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.affirm.android.model.Promo;
import com.affirm.android.model.PromoConfig;
import com.affirm.android.model.PromoResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowWebView;

import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AffirmPromotionButtonRenderTest {

    private static final String NATIVE = "Pay over time with affirm";
    private static final String HTML = "<span>Pay over time</span>";

    private AffirmPromotionButton button;

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0",
                    Affirm.Environment.SANDBOX).build());
        }
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        button = new AffirmPromotionButton(activity);
        activity.setContentView(button);
    }

    private static PromoResponse response(String ala, String htmlAla) {
        return response(ala, htmlAla, true);
    }

    private static PromoResponse response(String ala, String htmlAla, boolean migrate) {
        return PromoResponse.builder()
                .setPromo(Promo.builder()
                        .setAla(ala)
                        .setHtmlAla(htmlAla)
                        .setPromoConfig(PromoConfig.builder()
                                .setPromoPrequalEnabled(false)
                                .setMigrateNativeToHtmlRenderValue(migrate)
                                .build())
                        .build())
                .build();
    }

    private PromoRenderPlan migrated(String ala, String htmlAla) {
        return PromoRenderPlan.create(false, false, button.nativeTypeface(), response(ala, htmlAla));
    }

    private PromoRenderPlan explicitHtml() {
        button.configWithHtmlStyling();
        return PromoRenderPlan.create(true, false, button.nativeTypeface(), response(NATIVE, HTML));
    }

    private View shown() {
        assertThat(button.getChildCount()).isEqualTo(1);
        return button.getChildAt(0);
    }

    private static void ready(PromotionWebView webView) {
        webView.onScriptMessage("{\"event\":\"ready\",\"renderId\":\""
                + webView.renderId() + "\",\"height\":20}");
        ShadowLooper.idleMainLooper();
    }

    private static void fail(PromotionWebView webView) {
        webView.onRendererGone();
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void emptyCopyHidesTheButton() {
        button.render(migrated("", ""));

        assertThat(button.getVisibility()).isEqualTo(View.GONE);
        assertThat(button.getChildCount()).isEqualTo(0);
        assertThat(button.isEmpty()).isTrue();
    }

    @Test
    public void nativePlanShowsNativeCopy() {
        button.render(PromoRenderPlan.create(false, true, null, response(NATIVE, HTML)));

        View view = shown();
        assertThat(view).isInstanceOf(PromotionButton.class);
        assertThat(((PromotionButton) view).getText().toString()).contains("Pay over time");
        assertThat(view.getContentDescription().toString()).isEqualTo(NATIVE);
        assertThat(button.getPromotionWebView()).isNull();
    }

    @Test
    public void migratedHtmlLooksLikeTheNativeText() {
        assertThat(button.nativeTypeface()).isEqualTo(Typeface.SERIF);

        button.render(migrated(NATIVE, HTML));

        PromotionWebView webView = button.getPromotionWebView();
        assertThat(shown()).isSameAs(webView);
        PromotionButton nativeButton = new PromotionButton(button.getContext());
        nativeButton.setAffirmTextSize(button.getResources()
                .getDimensionPixelSize(R.dimen.affirm_promotion_size));
        nativeButton.setAffirmTextColor(android.R.color.black);
        String css = PromoHtmlStyle.migratedCss(Typeface.SERIF,
                nativeButton.getCurrentTextColor(), nativeButton.getTextSize(),
                nativeButton.getGravity(), button.getResources().getDisplayMetrics().density);
        String page = ((ShadowWebView) Shadow.extract(webView)).getLastLoadDataWithBaseURL().data;
        assertThat(page).contains(AffirmPromoRendering.fontDeclaration(css).css);
        assertThat(webView.getSettings().getTextZoom()).isEqualTo(100);
    }

    @Test
    public void htmlIsReadAsOneButton() {
        button.render(migrated(NATIVE, HTML));

        PromotionWebView webView = button.getPromotionWebView();
        assertThat(button.getContentDescription().toString()).isEqualTo(NATIVE);
        assertThat(webView.isFocusable()).isFalse();
        assertThat(webView.getImportantForAccessibility())
                .isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        assertThat(button.isFocusable()).isTrue();
        assertThat(button.getAccessibilityClassName().toString())
                .isEqualTo(Button.class.getName());

        button.render(PromoRenderPlan.create(false, true, null, response(NATIVE, HTML)));

        assertThat(button.getContentDescription()).isNull();
        assertThat(button.isFocusable()).isFalse();
        assertThat(button.getAccessibilityClassName().toString())
                .isNotEqualTo(Button.class.getName());
    }

    @Test
    public void enterOnShownHtmlClicksOnce() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        button.setOnClickListener(listener);
        button.render(migrated(NATIVE, HTML));
        assertThat(button.requestFocus()).isTrue();

        pressEnter();
        verify(listener, never()).onClick(any(View.class));

        ready(button.getPromotionWebView());
        pressEnter();
        verify(listener, times(1)).onClick(any(View.class));
    }

    private void pressEnter() {
        button.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        button.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        ShadowLooper.idleMainLooper();
    }

    @Test
    public void failedExplicitHtmlIsNotFocusable() {
        button.render(explicitHtml());

        fail(button.getPromotionWebView());

        assertThat(button.isFocusable()).isFalse();
    }

    @Test
    public void migratedHtmlTimeoutFallsBackToNative() {
        button.render(migrated(NATIVE, HTML));

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(shown()).isInstanceOf(PromotionButton.class);
        assertThat(button.getPromotionWebView()).isNull();
    }

    @Test
    public void readyHtmlDoesNotTimeOut() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        ready(webView);

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(shown()).isSameAs(webView);
        assertThat(webView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void clearedButtonIgnoresAPendingTimeout() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        button.showPlaceholder(null);

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(shown()).isSameAs(webView);
        assertThat(webView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void clearedButtonKeepsItsWebViewForTheNextHtml() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        ready(webView);
        String page = page(webView);

        button.showPlaceholder(null);
        button.render(migrated(NATIVE, HTML + "<br>"));

        assertThat(shown()).isSameAs(webView);
        assertThat(page(webView)).isSameAs(page);
        assertThat(script(webView)).contains("affirmPromoRender");
    }

    @Test
    public void migratedHtmlFailureBeforeReadyFallsBackToNative() {
        button.render(migrated(NATIVE, HTML));

        fail(button.getPromotionWebView());

        View view = shown();
        assertThat(view).isInstanceOf(PromotionButton.class);
        assertThat(view.getContentDescription().toString()).isEqualTo(NATIVE);
        assertThat(button.getPromotionWebView()).isNull();
    }

    @Test
    public void migratedHtmlFailureAfterReadyHidesTheButton() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        ready(webView);
        assertThat(webView.getVisibility()).isEqualTo(View.VISIBLE);

        fail(webView);

        assertThat(button.getChildCount()).isEqualTo(0);
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
        assertThat(button.isEmpty()).isTrue();
        assertThat(button.getContentDescription()).isNull();
    }

    @Test
    public void explicitHtmlFailureNeverShowsNative() {
        button.render(explicitHtml());

        fail(button.getPromotionWebView());

        assertThat(button.getChildCount()).isEqualTo(0);
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void nextHtmlRendersInTheLoadedPage() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        ready(webView);
        String page = page(webView);

        button.render(migrated(NATIVE, HTML + "<br>"));

        assertThat(button.getPromotionWebView()).isSameAs(webView);
        assertThat(page(webView)).isSameAs(page);
        assertThat(script(webView)).contains("affirmPromoRender");
        assertThat(webView.getAlpha()).isEqualTo(0f);
        ready(webView);
        assertThat(webView.getAlpha()).isEqualTo(1f);
    }

    @Test
    public void failureOfAReplacedRenderIsIgnored() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView webView = button.getPromotionWebView();
        String first = webView.renderId();
        button.render(migrated(NATIVE, HTML + "<br>"));

        webView.onScriptMessage("{\"event\":\"failure\",\"renderId\":\"" + first + "\"}");
        ShadowLooper.idleMainLooper();

        assertThat(shown()).isSameAs(webView);
        ready(webView);
        assertThat(webView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void failedHtmlStartsTheNextOneInANewWebView() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView failed = button.getPromotionWebView();
        fail(failed);

        button.render(migrated(NATIVE, HTML + "<br>"));

        assertThat(button.getPromotionWebView()).isNotSameAs(failed);
        assertThat(shown()).isSameAs(button.getPromotionWebView());
    }

    @Test
    public void clickListenerReachesTheRenderedWebView() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        button.setOnClickListener(listener);

        button.render(migrated(NATIVE, HTML));
        ready(button.getPromotionWebView());
        button.getPromotionWebView().performClick();

        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void tapBeforeThePromoShowsDoesNothing() {
        View.OnClickListener listener = mock(View.OnClickListener.class);
        button.setOnClickListener(listener);

        button.performClick();
        button.render(migrated(NATIVE, HTML));
        button.performClick();
        verify(listener, never()).onClick(any(View.class));

        ready(button.getPromotionWebView());
        button.performClick();
        verify(listener, times(1)).onClick(any(View.class));
    }

    @Test
    public void sameResponseDoesNotReload() {
        button.showResponse(response(NATIVE, HTML), true);
        PromotionWebView webView = button.getPromotionWebView();

        button.showResponse(response(NATIVE, HTML), false);

        assertThat(button.getPromotionWebView()).isSameAs(webView);
        assertThat(button.showsPrequal()).isFalse();
    }

    @Test
    public void newLocalStylingAppliesToTheSameResponse() {
        button.showResponse(response(NATIVE, HTML, false), false);
        PromotionButton nativeButton = (PromotionButton) shown();

        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_WHITE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT, Typeface.MONOSPACE);
        button.showResponse(response(NATIVE, HTML, false), false);

        assertThat(shown()).isSameAs(nativeButton);
        assertThat(nativeButton.getTypeface()).isEqualTo(Typeface.MONOSPACE);
        assertThat(nativeButton.getText().toString()).isEqualTo(NATIVE);
    }

    @Test
    public void newHtmlStylingReloadsTheSameResponse() {
        button.configWithHtmlStyling();
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView first = button.getPromotionWebView();

        button.configWithHtmlStyling(null, "<style>body{color:red}</style>");
        button.showResponse(response(NATIVE, HTML), false);

        assertThat(button.getPromotionWebView()).isSameAs(first);
        assertThat(page(first)).contains("body{color:red}");
    }

    @Test
    public void switchingMigratedHtmlToExplicitHtmlReloadsWithoutNativeFallback() {
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView migrated = button.getPromotionWebView();

        button.configWithHtmlStyling();
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView explicit = button.getPromotionWebView();
        assertThat(explicit).isNotSameAs(migrated);
        assertThat(page(migrated)).contains("#affirm-promo-root{");
        assertThat(page(explicit)).doesNotContain("#affirm-promo-root{");

        fail(explicit);

        assertThat(button.getChildCount()).isEqualTo(0);
        assertThat(button.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void switchingExplicitHtmlToLocalStylingDropsTheMerchantCss() {
        button.configWithHtmlStyling("https://cdn.test/a.css");
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView explicit = button.getPromotionWebView();
        assertThat(page(explicit)).contains("https://cdn.test/a.css");

        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, Typeface.SERIF);
        button.showResponse(response(NATIVE, HTML), false);

        PromotionWebView migrated = button.getPromotionWebView();
        assertThat(migrated).isNotSameAs(explicit);
        assertThat(page(migrated)).doesNotContain("https://cdn.test/a.css");
        assertThat(page(migrated)).contains("font-family:serif");
    }

    @Test
    public void newLocalStylingRestylesMigratedHtml() {
        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, Typeface.SERIF);
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView first = button.getPromotionWebView();

        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, Typeface.MONOSPACE);
        button.showResponse(response(NATIVE, HTML), false);

        assertThat(button.getPromotionWebView()).isSameAs(first);
        assertThat(page(first)).contains("font-family:monospace");
    }

    @Test
    public void sameLocalStylingKeepsMigratedHtml() {
        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, Typeface.SERIF);
        button.showResponse(response(NATIVE, HTML), false);
        PromotionWebView first = button.getPromotionWebView();

        button.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, Typeface.SERIF);
        button.showResponse(response(NATIVE, HTML), false);

        assertThat(button.getPromotionWebView()).isSameAs(first);
    }

    private static void readyWithWidth(PromotionWebView webView, int cssWidth) {
        readyWithWidth(webView, cssWidth, cssWidth);
    }

    private static void readyWithWidth(PromotionWebView webView, int cssWidth, int cssLineWidth) {
        webView.onScriptMessage("{\"event\":\"ready\",\"renderId\":\"" + webView.renderId()
                + "\",\"height\":20,\"width\":" + cssWidth + ",\"lineWidth\":" + cssLineWidth + "}");
        ShadowLooper.idleMainLooper();
    }

    private int px(int cssPixels) {
        return AffirmPromoRendering.heightForCssPixels(cssPixels,
                button.getResources().getDisplayMetrics().density);
    }

    private int measuredWidth(int widthMeasureSpec) {
        button.measure(widthMeasureSpec,
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return button.getMeasuredWidth();
    }

    @Test
    public void migratedHtmlWrapsItsContentWidthLikeNative() {
        button.render(migrated(NATIVE, HTML));
        readyWithWidth(button.getPromotionWebView(), 120);

        assertThat(measuredWidth(View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST)))
                .isEqualTo(px(120));
        assertThat(measuredWidth(View.MeasureSpec.makeMeasureSpec(50, View.MeasureSpec.AT_MOST)))
                .isEqualTo(50);
        assertThat(measuredWidth(View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)))
                .isEqualTo(2000);
    }

    @Test
    public void wrappedMigratedHtmlWrapsItsWidestLine() {
        button.render(migrated(NATIVE, HTML));
        readyWithWidth(button.getPromotionWebView(), 400, 230);

        assertThat(measuredWidth(View.MeasureSpec.makeMeasureSpec(px(250),
                View.MeasureSpec.AT_MOST))).isEqualTo(px(230));
        assertThat(measuredWidth(View.MeasureSpec.makeMeasureSpec(px(500),
                View.MeasureSpec.AT_MOST))).isEqualTo(px(400));
    }

    @Test
    public void explicitHtmlKeepsTheWidthItIsGiven() {
        button.render(explicitHtml());
        int atMost = View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.AT_MOST);
        int beforeReady = measuredWidth(atMost);
        assertThat(beforeReady).isNotEqualTo(px(120));

        readyWithWidth(button.getPromotionWebView(), 120);

        assertThat(measuredWidth(atMost)).isEqualTo(beforeReady);
    }

    private static int horizontalGravity(View view) {
        return ((TextView) view).getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
    }

    @Test
    public void textGravityCentersNativeText() {
        button.render(PromoRenderPlan.create(false, true, null, response(NATIVE, HTML)));
        assertThat(horizontalGravity(shown())).isEqualTo(Gravity.LEFT);

        button.setTextGravity(Gravity.CENTER_HORIZONTAL);

        assertThat(horizontalGravity(shown())).isEqualTo(Gravity.CENTER_HORIZONTAL);
    }

    @Test
    public void xmlGravityCentersNativeText() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AffirmPromotionButton centered = new AffirmPromotionButton(activity,
                Robolectric.buildAttributeSet()
                        .addAttribute(android.R.attr.gravity, "center_horizontal").build());
        activity.setContentView(centered);

        centered.render(PromoRenderPlan.create(false, true, null, response(NATIVE, HTML)));

        assertThat(horizontalGravity(centered.getChildAt(0)))
                .isEqualTo(Gravity.CENTER_HORIZONTAL);
    }

    @Test
    public void xmlHtmlStylingBuildsItsWebViewForSetLabel() {
        AffirmPromotionButton xmlHtml = xmlHtmlStylingButton();

        xmlHtml.setLabel(NATIVE, NATIVE);

        assertThat(xmlHtml.getPromotionWebView()).isNotNull();
        assertThat(xmlHtml.getChildAt(0)).isSameAs(xmlHtml.getPromotionWebView());
    }

    @Test
    public void xmlHtmlStylingBuildsItsWebViewForAManagedRender() {
        AffirmPromotionButton xmlHtml = xmlHtmlStylingButton();
        assertThat(xmlHtml.isHtmlStyle()).isTrue();

        xmlHtml.render(PromoRenderPlan.create(xmlHtml.isHtmlStyle(), false, null,
                response(NATIVE, HTML)));

        assertThat(xmlHtml.getPromotionWebView()).isNotNull();
        assertThat(xmlHtml.getChildAt(0)).isSameAs(xmlHtml.getPromotionWebView());
    }

    /** Configured for HTML only by the XML attribute, without configWithHtmlStyling. */
    private AffirmPromotionButton xmlHtmlStylingButton() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        AffirmPromotionButton xmlHtml = new AffirmPromotionButton(activity,
                Robolectric.buildAttributeSet()
                        .addAttribute(R.attr.htmlStyling, "true").build());
        activity.setContentView(xmlHtml);
        return xmlHtml;
    }

    @Test
    public void textGravityCentersMigratedHtml() {
        button.setTextGravity(Gravity.CENTER_HORIZONTAL);

        button.render(migrated(NATIVE, HTML));

        assertThat(page(button.getPromotionWebView())).contains("text-align:center");
    }

    private static String page(PromotionWebView webView) {
        return ((ShadowWebView) Shadow.extract(webView)).getLastLoadDataWithBaseURL().data;
    }

    private static String script(PromotionWebView webView) {
        return ((ShadowWebView) Shadow.extract(webView)).getLastEvaluatedJavascript();
    }

    @Test
    public void destroyedButtonShowsTheSameResponseAgain() {
        button.showResponse(response(NATIVE, HTML), false);
        button.destroy();
        assertThat(button.getChildCount()).isEqualTo(0);

        button.showResponse(response(NATIVE, HTML), false);

        assertThat(shown()).isSameAs(button.getPromotionWebView());
    }

    @Test
    public void migratedPlaceholderShowsHtml() {
        button.showPlaceholder(new PromoMessageCache.CachedMessage(NATIVE, HTML, true, true));

        assertThat(shown()).isSameAs(button.getPromotionWebView());
        assertThat(button.showsPrequal()).isTrue();
    }
}

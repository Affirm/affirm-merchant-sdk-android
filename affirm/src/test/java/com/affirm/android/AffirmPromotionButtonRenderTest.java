package com.affirm.android;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;

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

        button.render(PromoRenderPlan.create(false, true, null, response(NATIVE, HTML)));

        assertThat(button.getContentDescription()).isNull();
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
        button.showPlaceholder(null);

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertThat(button.getChildCount()).isEqualTo(0);
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
    public void failureFromAReplacedWebViewIsIgnored() {
        button.render(migrated(NATIVE, HTML));
        PromotionWebView first = button.getPromotionWebView();
        button.render(migrated(NATIVE, HTML + "<br>"));
        PromotionWebView second = button.getPromotionWebView();

        fail(first);

        assertThat(second).isNotSameAs(first);
        assertThat(shown()).isSameAs(second);
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

        assertThat(button.getPromotionWebView()).isNotSameAs(first);
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

        PromotionWebView second = button.getPromotionWebView();
        assertThat(second).isNotSameAs(first);
        assertThat(page(second)).contains("font-family:monospace");
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

    private static String page(PromotionWebView webView) {
        return ((ShadowWebView) Shadow.extract(webView)).getLastLoadDataWithBaseURL().data;
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

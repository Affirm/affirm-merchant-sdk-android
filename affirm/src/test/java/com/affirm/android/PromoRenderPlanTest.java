package com.affirm.android;

import android.graphics.Typeface;

import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.Promo;
import com.affirm.android.model.PromoConfig;
import com.affirm.android.model.PromoResponse;
import com.google.gson.GsonBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PromoRenderPlanTest {

    private static final String NATIVE = "Pay over time with affirm";
    private static final String HTML = "<span>Pay over time</span>";

    private static PromoResponse response(String ala, String htmlAla, Boolean flag) {
        return PromoResponse.builder()
                .setPromo(Promo.builder()
                        .setAla(ala)
                        .setHtmlAla(htmlAla)
                        .setPromoConfig(PromoConfig.builder()
                                .setPromoPrequalEnabled(false)
                                .setMigrateNativeToHtmlRenderValue(flag)
                                .build())
                        .build())
                .build();
    }

    private static final String REAL_HTML_ALA = "<span class='__apr_badge __apr_badge_indigo'>"
            + "0% APR </span>or as low as <span class='affirm-ala-price'>$54</span>/mo with "
            + "<span class='__affirm-logo __affirm-logo-blue'>Affirm</span>. "
            + "<a class='affirm-modal-trigger'>Learn more</a>";

    // A sandbox promos response body, as returned. It has no migration flag.
    private static final String REAL_RESPONSE = "{\"promo\":{"
            + "\"ala\":\"0% APR or as low as $54/mo with {affirm_logo}. Learn more\","
            + "\"html_ala\":\"" + REAL_HTML_ALA + "\","
            + "\"config\":{\"promo_prequal_enabled\":false,\"promo_style\":\"adaptive\","
            + "\"merchant_ari\":\"C8QU0QPNAD4VRGK3\",\"loan_type\":\"installment\","
            + "\"user_ari\":\"\",\"toast_enabled\":false,\"enabled_integrations\":[]}}}";

    private static PromoResponse parse(String json) {
        return new GsonBuilder()
                .registerTypeAdapterFactory(AffirmAdapterFactory.create())
                .create()
                .fromJson(json, PromoResponse.class);
    }

    private static PromoRenderPlan migrated(PromoResponse response) {
        return PromoRenderPlan.create(false, false, Typeface.DEFAULT, response);
    }

    private static PromoRenderPlan explicitHtml(PromoResponse response) {
        return PromoRenderPlan.create(true, false, Typeface.DEFAULT, response);
    }

    @Test
    public void flagOnShowsHtmlWithNativeFallback() {
        PromoRenderPlan plan = migrated(response(NATIVE, HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.text).isEqualTo(HTML);
        assertThat(plan.description).isEqualTo(NATIVE);
        assertThat(plan.canFallBackToNative).isTrue();
    }

    @Test
    public void flagOffShowsNative() {
        PromoRenderPlan plan = migrated(response(NATIVE, HTML, false));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.text).isEqualTo(NATIVE);
        assertThat(plan.description).isEqualTo(NATIVE);
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void missingFlagShowsNative() {
        PromoRenderPlan plan = migrated(response(NATIVE, HTML, null));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.text).isEqualTo(NATIVE);
    }

    @Test
    public void nativeCopyReplacesTheLogoPlaceholderButHtmlDoesNot() {
        PromoRenderPlan nativePlan =
                migrated(response("Pay with {affirm_logo}", "<b>{affirm_logo}</b>", false));
        assertThat(nativePlan.text).isEqualTo("Pay with affirm");

        PromoRenderPlan htmlPlan =
                migrated(response("Pay with {affirm_logo}", "<b>{affirm_logo}</b>", true));
        assertThat(htmlPlan.text).isEqualTo("<b>{affirm_logo}</b>");
        assertThat(htmlPlan.description).isEqualTo("Pay with affirm");
    }

    @Test
    public void flagOnWithoutHtmlShowsNativeFromSameResponse() {
        PromoRenderPlan plan = migrated(response(NATIVE, " ", true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.text).isEqualTo(NATIVE);
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void flagOnWithoutAnyCopyShowsEmptyHtml() {
        PromoRenderPlan plan = migrated(response("", "", true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.text).isEmpty();
        assertThat(plan.description).isEmpty();
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void flagOnWithBlankNativeShowsHtmlWithoutFallback() {
        PromoRenderPlan plan = migrated(response(" ", HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void nativeWithoutCopyShowsEmptyNative() {
        PromoRenderPlan plan = migrated(response("", HTML, false));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.text).isEmpty();
    }

    @Test
    public void explicitHtmlShowsHtmlWithoutFallback() {
        PromoRenderPlan plan = explicitHtml(response(NATIVE, HTML, false));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.text).isEqualTo(HTML);
        assertThat(plan.description).isEqualTo(NATIVE);
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void explicitHtmlWithoutHtmlStaysHtmlEvenWithNativeCopy() {
        PromoRenderPlan plan = explicitHtml(response(NATIVE, "", false));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.text).isEmpty();
        assertThat(plan.canFallBackToNative).isFalse();
        assertThat(plan.description).isEqualTo(NATIVE);
    }

    @Test
    public void optOutStaysNative() {
        PromoRenderPlan plan =
                PromoRenderPlan.create(false, true, Typeface.DEFAULT, response(NATIVE, HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
    }

    @Test
    public void customFontStaysNative() {
        Typeface custom = mock(Typeface.class);
        when(custom.getStyle()).thenReturn(Typeface.NORMAL);
        PromoRenderPlan plan =
                PromoRenderPlan.create(false, false, custom, response(NATIVE, HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
    }

    @Test
    public void realResponseWithoutTheFlagShowsNative() {
        PromoResponse response = parse(REAL_RESPONSE);
        assertThat(response.promo().promoConfig().promoStyle()).isEqualTo("adaptive");
        PromoRenderPlan plan = migrated(response);
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.text).isEqualTo("0% APR or as low as $54/mo with affirm. Learn more");
        assertThat(plan.canFallBackToNative).isFalse();
    }

    @Test
    public void realResponseWithTheFlagOnShowsItsHtmlWithNativeFallback() {
        PromoRenderPlan plan = migrated(parse(REAL_RESPONSE.replace("\"config\":{",
                "\"config\":{\"android_sdk_migrate_native_to_html_render\":true,")));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.text).isEqualTo(REAL_HTML_ALA);
        assertThat(plan.description)
                .isEqualTo("0% APR or as low as $54/mo with affirm. Learn more");
        assertThat(plan.canFallBackToNative).isTrue();
    }

    @Test
    public void blankHtmlRecordsTheFallbackToNative() {
        PromoRenderPlan plan = migrated(response(NATIVE, " ", true));
        assertThat(plan.selected).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.mode).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(plan.fallbackReason).isEqualTo(AffirmPromoRenderReason.MISSING_HTML);
        assertThat(plan.nativeUsable).isTrue();
        assertThat(plan.htmlPresent).isFalse();
    }

    @Test
    public void shownHtmlHasNoFallbackReason() {
        PromoRenderPlan plan = migrated(response(NATIVE, HTML, true));
        assertThat(plan.selected).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.fallbackReason).isNull();
        assertThat(plan.explicitHtml).isFalse();
        assertThat(plan.htmlPresent).isTrue();
    }

    @Test
    public void explicitHtmlIsRecorded() {
        PromoRenderPlan plan = explicitHtml(response(" ", HTML, false));
        assertThat(plan.explicitHtml).isTrue();
        assertThat(plan.selected).isEqualTo(PromoRendererMode.HTML);
        assertThat(plan.nativeUsable).isFalse();
    }

    @Test
    public void defaultFontCanMigrate() {
        PromoRenderPlan plan = PromoRenderPlan.create(false, false, null, response(NATIVE, HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
    }
}

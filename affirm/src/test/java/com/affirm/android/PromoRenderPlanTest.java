package com.affirm.android;

import android.graphics.Typeface;

import com.affirm.android.model.Promo;
import com.affirm.android.model.PromoConfig;
import com.affirm.android.model.PromoResponse;

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
    public void defaultFontCanMigrate() {
        PromoRenderPlan plan = PromoRenderPlan.create(false, false, null, response(NATIVE, HTML, true));
        assertThat(plan.mode).isEqualTo(PromoRendererMode.HTML);
    }
}

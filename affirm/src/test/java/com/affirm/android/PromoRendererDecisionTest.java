package com.affirm.android;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PromoRendererDecisionTest {

    @Test
    public void explicitHtmlWins() {
        PromoRendererDecision.Result result = PromoRendererDecision.decide(
                true, true, false, false);
        assertThat(result.selected).isEqualTo(PromoRendererMode.HTML);
        assertThat(result.decisionReason).isEqualTo("explicit_html");
    }

    @Test
    public void legacyOptOutKeepsNative() {
        PromoRendererDecision.Result result = PromoRendererDecision.decide(
                false, true, true, true);
        assertThat(result.selected).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(result.decisionReason).isEqualTo("opt_out");
    }

    @Test
    public void unsupportedFontKeepsNative() {
        PromoRendererDecision.Result result = PromoRendererDecision.decide(
                false, false, false, true);
        assertThat(result.selected).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(result.decisionReason).isEqualTo("unsupported_font");
    }

    @Test
    public void flagOnSelectsHtml() {
        PromoRendererDecision.Result result = PromoRendererDecision.decide(
                false, false, true, true);
        assertThat(result.selected).isEqualTo(PromoRendererMode.HTML);
        assertThat(result.decisionReason).isEqualTo("flag_true");
    }

    @Test
    public void flagOffStaysNative() {
        PromoRendererDecision.Result result = PromoRendererDecision.decide(
                false, false, true, false);
        assertThat(result.selected).isEqualTo(PromoRendererMode.NATIVE);
        assertThat(result.decisionReason).isEqualTo("flag_false");
    }
}

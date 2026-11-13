package com.affirm.android;

import android.graphics.Typeface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void builtInFontsAreSupported() {
        assertThat(PromoRendererDecision.isFontSupported(Typeface.DEFAULT)).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(Typeface.DEFAULT_BOLD)).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(Typeface.SANS_SERIF)).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(Typeface.SERIF)).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(Typeface.MONOSPACE)).isTrue();
    }

    @Test
    public void boldAndItalicBuiltInFontsAreSupported() {
        assertThat(PromoRendererDecision.isFontSupported(
                Typeface.create(Typeface.SERIF, Typeface.ITALIC))).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(
                Typeface.create(Typeface.MONOSPACE, Typeface.BOLD))).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(
                Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC))).isTrue();
        assertThat(PromoRendererDecision.isFontSupported(
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD))).isTrue();
    }

    @Test
    public void nullIsTheDefaultFont() {
        assertThat(PromoRendererDecision.isFontSupported(null)).isTrue();
    }

    @Test
    public void customFontsAreUnsupported() {
        Typeface custom = mock(Typeface.class);
        when(custom.getStyle()).thenReturn(Typeface.NORMAL);
        assertThat(PromoRendererDecision.isFontSupported(custom)).isFalse();
    }
}

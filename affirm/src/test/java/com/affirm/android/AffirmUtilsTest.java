package com.affirm.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.style.ImageSpan;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class AffirmUtilsTest {

    private Context mockContext;

    @Before
    public void setUp() {
        // Create a mock Context and Resources so that drawable loading returns
        // a fake Drawable instead of trying to inflate vector XML resources,
        // which Robolectric cannot always resolve.
        mockContext = mock(Context.class);
        Resources mockResources = mock(Resources.class);
        Drawable mockDrawable = mock(Drawable.class);

        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDrawable(anyInt())).thenReturn(mockDrawable);
        when(mockDrawable.mutate()).thenReturn(mockDrawable);
        when(mockDrawable.getIntrinsicWidth()).thenReturn(100);
        when(mockDrawable.getIntrinsicHeight()).thenReturn(40);
    }

    @Test
    public void convertToAffirmAmounts() {
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(15.5))).isEqualTo(1550);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(15.5492))).isEqualTo(1554);
        Truth.assertThat(AffirmUtils.decimalDollarsToIntegerCents(BigDecimal.valueOf(3.0))).isEqualTo(300);
    }

    @Test
    public void replacePlaceHolders() {
        Map<String, String> map = ImmutableMap.of("money", "55", "name", "jan", "day", "monday");
        String text = "I paid {{money}} to {{name}} last {{day}}";

        Truth.assertThat(AffirmUtils.replacePlaceholders(text, map))
                .contains("I paid 55 to jan last monday");
    }

    @Test
    public void createSpannable_onlyReplacesPlaceholderWithLogo() {
        // Template contains one {affirm_logo} placeholder and one plain text "Affirm"
        String template = "Pay over time with {affirm_logo}. Affirm is a form of credit.";

        SpannableString result = AffirmUtils.createSpannableForText(
                template,
                14f,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                mockContext
        );

        // Only 1 ImageSpan should be present (for the placeholder), not 2
        ImageSpan[] spans = result.getSpans(0, result.length(), ImageSpan.class);
        Truth.assertThat(spans).hasLength(1);

        // The plain text "Affirm" should remain as text in the output
        String resultText = result.toString();
        Truth.assertThat(resultText).contains("Affirm is a form of credit.");
    }

    @Test
    public void createSpannable_plainTextAffirmNotReplacedWithLogo() {
        // Template with ONLY plain text "Affirm" and no placeholder
        String template = "Affirm offers great financing options.";

        SpannableString result = AffirmUtils.createSpannableForText(
                template,
                14f,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                mockContext
        );

        // No ImageSpan should be present since there is no {affirm_logo} placeholder
        ImageSpan[] spans = result.getSpans(0, result.length(), ImageSpan.class);
        Truth.assertThat(spans).hasLength(0);

        // The text should remain unchanged
        Truth.assertThat(result.toString()).isEqualTo(template);
    }

    @Test
    public void createSpannable_multiplePlaceholdersAllReplaced() {
        // Template with multiple {affirm_logo} placeholders
        String template = "Pay with {affirm_logo} or learn about {affirm_logo} financing.";

        SpannableString result = AffirmUtils.createSpannableForText(
                template,
                14f,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                mockContext
        );

        // Both placeholders should get replaced with logo ImageSpans
        ImageSpan[] spans = result.getSpans(0, result.length(), ImageSpan.class);
        Truth.assertThat(spans).hasLength(2);
    }

    @Test
    public void createSpannable_textDisplayType_noLogosInserted() {
        // When display type is TEXT, no logos should be inserted even with placeholders
        String template = "Pay over time with {affirm_logo}.";

        SpannableString result = AffirmUtils.createSpannableForText(
                template,
                14f,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT,
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                mockContext
        );

        ImageSpan[] spans = result.getSpans(0, result.length(), ImageSpan.class);
        Truth.assertThat(spans).hasLength(0);

        // The placeholder should be replaced with text "Affirm", not left as raw {affirm_logo}
        String resultText = result.toString();
        Truth.assertThat(resultText).doesNotContain("{affirm_logo}");
        Truth.assertThat(resultText).contains("Pay over time with Affirm.");
    }
}

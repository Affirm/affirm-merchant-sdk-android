package com.affirm.android;

import com.google.common.truth.Truth;

import org.junit.Test;

public class AffirmLogoTypeTest {

    @Test
    public void getDrawableResTest() {
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLACK)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_WHITE)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK)).isEqualTo(R.drawable.affirm_blue_black_logo_transparent_bg);

        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLACK)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_WHITE)).isEqualTo(R.drawable.affirm_black_logo_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK)).isEqualTo(R.drawable.affirm_blue_black_logo_transparent_bg);

        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLACK)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_WHITE)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK)).isEqualTo(R.drawable.affirm_blue_black_hollow_circle_transparent_bg);

        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLACK)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_WHITE)).isEqualTo(R.drawable.affirm_black_hollow_circle_transparent_bg);
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW.getDrawableRes(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK)).isEqualTo(R.drawable.affirm_blue_black_hollow_circle_transparent_bg);
    }

    @Test
    public void getTypeTest() {
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO.getType()).isEqualTo("logo");
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT.getType()).isEqualTo("text");
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL.getType()).isEqualTo("symbol");
        Truth.assertThat(AffirmLogoType.AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW.getType()).isEqualTo("symbol");
    }
}

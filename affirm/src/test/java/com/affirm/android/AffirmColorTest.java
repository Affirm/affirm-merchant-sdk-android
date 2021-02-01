package com.affirm.android;

import com.google.common.truth.Truth;

import org.junit.Test;

public class AffirmColorTest {

    @Test
    public void getColorResTest() {
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLUE.getColorRes()).isEqualTo(R.color.affirm_blue);
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLACK.getColorRes()).isEqualTo(R.color.affirm_black);
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_WHITE.getColorRes()).isEqualTo(R.color.affirm_white);
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK.getColorRes()).isEqualTo(R.color.affirm_blue);
    }

    @Test
    public void getColorTest() {
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLUE.getColor()).isEqualTo("blue");
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLACK.getColor()).isEqualTo("black");
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_WHITE.getColor()).isEqualTo("white");
        Truth.assertThat(AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK.getColor()).isEqualTo("blue");
    }
}

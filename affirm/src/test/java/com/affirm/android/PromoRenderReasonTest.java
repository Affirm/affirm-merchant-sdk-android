package com.affirm.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class PromoRenderReasonTest {

    @Test
    public void fromPageKeepsKnownCodes() {
        assertThat(PromoRenderReason.fromPage(PromoRenderReason.HTML_EMPTY))
                .isEqualTo(PromoRenderReason.HTML_EMPTY);
        assertThat(PromoRenderReason.fromPage(PromoRenderReason.SANITIZER_UNAVAILABLE))
                .isEqualTo(PromoRenderReason.SANITIZER_UNAVAILABLE);
        assertThat(PromoRenderReason.fromPage(PromoRenderReason.RESOURCE_FAILED))
                .isEqualTo(PromoRenderReason.RESOURCE_FAILED);
        assertThat(PromoRenderReason.fromPage(PromoRenderReason.IMAGE_FAILED))
                .isEqualTo(PromoRenderReason.IMAGE_FAILED);
        assertThat(PromoRenderReason.fromPage(PromoRenderReason.HTML_TIMEOUT))
                .isEqualTo(PromoRenderReason.HTML_TIMEOUT);
    }

    @Test
    public void fromPageRejectsUnknownCodes() {
        assertThat(PromoRenderReason.fromPage(null)).isEqualTo(PromoRenderReason.SCRIPT_ERROR);
        assertThat(PromoRenderReason.fromPage("")).isEqualTo(PromoRenderReason.SCRIPT_ERROR);
        assertThat(PromoRenderReason.fromPage("load_error"))
                .isEqualTo(PromoRenderReason.SCRIPT_ERROR);
        assertThat(PromoRenderReason.fromPage("<script>"))
                .isEqualTo(PromoRenderReason.SCRIPT_ERROR);
    }
}

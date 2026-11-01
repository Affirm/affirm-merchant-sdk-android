package com.affirm.android;

import android.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.charset.StandardCharsets;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class PromoHtmlLayoutTest {

    @Test
    public void contentHeightConvertsCssPxToDevicePx() {
        assertThat(PromotionWebView.cssToDevicePx(40, 3f)).isEqualTo(120);
        assertThat(PromotionWebView.cssToDevicePx(41, 2.625f)).isEqualTo(108);
    }

    @Test
    public void pageEncodingIsUtf8Base64() {
        String html = "<span>\"$50\" — 中文</span>";
        String encoded = PromotionWebView.encodeForPage(html);

        assertThat(encoded).doesNotContain("<");
        assertThat(new String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8))
                .isEqualTo(html);
        assertThat(PromotionWebView.encodeForPage(null)).isEmpty();
    }

    @Test
    public void sameVisiblePromoDoesNotReload() {
        assertThat(PromotionWebView.shouldReload(
                android.view.View.VISIBLE, "<p>a</p>", "https://cdn.test/a.css", "font",
                "<p>a</p>", "https://cdn.test/a.css", "font")).isFalse();
        assertThat(PromotionWebView.shouldReload(
                android.view.View.INVISIBLE, "<p>a</p>", "https://cdn.test/a.css", "font",
                "<p>a</p>", "https://cdn.test/a.css", "font")).isTrue();
        assertThat(PromotionWebView.shouldReload(
                android.view.View.VISIBLE, "<p>b</p>", "https://cdn.test/a.css", "font",
                "<p>a</p>", "https://cdn.test/a.css", "font")).isTrue();
        assertThat(PromotionWebView.shouldReload(
                android.view.View.VISIBLE, "<p>a</p>", "https://cdn.test/a.css", "font",
                null, null, null)).isTrue();
    }
}

package com.affirm.android

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PromoHtmlStyleTest {

    private fun css(typeface: Typeface?) =
        PromoHtmlStyle.migratedCss(typeface, Color.BLACK, 28f, Gravity.CENTER, 2f)

    private fun customFont() = mock(Typeface::class.java).also {
        `when`(it.style).thenReturn(Typeface.NORMAL)
    }

    @Test
    fun builtInFontsAndNullAreSupported() {
        listOf(
            null, Typeface.DEFAULT, Typeface.DEFAULT_BOLD, Typeface.SANS_SERIF,
            Typeface.SERIF, Typeface.MONOSPACE,
            Typeface.create(Typeface.SERIF, Typeface.ITALIC),
            Typeface.create(Typeface.MONOSPACE, Typeface.BOLD),
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        ).forEach { assertThat(PromoHtmlStyle.isFontSupported(it)).isTrue() }
    }

    @Test
    fun customFontIsUnsupportedAndHasNoCss() {
        assertThat(PromoHtmlStyle.isFontSupported(customFont())).isFalse()
        assertThat(css(customFont())).isNull()
    }

    @Test
    fun supportedFontsMapToCss() {
        assertThat(css(null)).contains("font-family:sans-serif;font-weight:normal;font-style:normal")
        assertThat(css(Typeface.DEFAULT_BOLD))
            .contains("font-family:sans-serif;font-weight:bold;font-style:normal")
        assertThat(css(Typeface.SERIF))
            .contains("font-family:serif;font-weight:normal;font-style:normal")
        assertThat(css(Typeface.create(Typeface.SERIF, Typeface.ITALIC)))
            .contains("font-family:serif;font-weight:normal;font-style:italic")
        assertThat(css(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC)))
            .contains("font-family:monospace;font-weight:bold;font-style:italic")
    }

    @Test
    fun migratedCssCopiesTheNativeTextStyle() {
        assertThat(PromoHtmlStyle.migratedCss(Typeface.SERIF, 0x80FF0000.toInt(), 28f, Gravity.CENTER, 2f))
            .isEqualTo(
                "<style>body{margin:0;padding:0}#affirm-promo-root{margin:0;padding:0;" +
                    "font-family:serif;font-weight:normal;font-style:normal;" +
                    "color:rgba(255,0,0,0.5019608);font-size:14.0px;text-align:center;" +
                    "overflow-wrap:break-word}</style>"
            )
        assertThat(PromoHtmlStyle.migratedCss(null, Color.BLACK, 28f, Gravity.END or Gravity.CENTER_VERTICAL, 2f))
            .contains("text-align:end")
        assertThat(PromoHtmlStyle.migratedCss(null, Color.BLACK, 28f, Gravity.TOP, 2f))
            .contains("text-align:start")
    }
}

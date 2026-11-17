package com.affirm.android

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY

/** Turns the native promo text style into CSS for a migrated HTML render. */
@RestrictTo(LIBRARY)
internal object PromoHtmlStyle {

    /**
     * Built-in sans-serif / serif / monospace, including bold and italic, can be shown in HTML.
     * Custom Typefaces stay native. Null is the system default font.
     */
    @JvmStatic
    fun isFontSupported(typeface: Typeface?): Boolean = fontCss(typeface) != null

    /**
     * A `<style>` that makes the promo page look like the native text, or null when HTML can't
     * match [typeface]. [textSizePx] already includes the font scale, so the page needs textZoom 100.
     */
    @JvmStatic
    fun migratedCss(
        typeface: Typeface?,
        @ColorInt textColor: Int,
        textSizePx: Float,
        gravity: Int,
        density: Float
    ): String? {
        val font = fontCss(typeface) ?: return null
        val color = "rgba(${Color.red(textColor)},${Color.green(textColor)}," +
            "${Color.blue(textColor)},${Color.alpha(textColor) / 255f})"
        return "<style>body{margin:0;padding:0}#affirm-promo-root{margin:0;padding:0;$font;" +
            "color:$color;font-size:${textSizePx / density}px;text-align:${textAlign(gravity)};" +
            "overflow-wrap:break-word}</style>"
    }

    private fun fontCss(typeface: Typeface?): String? {
        if (typeface == null) return fontCss("sans-serif", Typeface.NORMAL)
        if (typeface == Typeface.DEFAULT_BOLD) return fontCss("sans-serif", Typeface.BOLD)
        val family = listOf(
            Typeface.DEFAULT to "sans-serif",
            Typeface.SANS_SERIF to "sans-serif",
            Typeface.SERIF to "serif",
            Typeface.MONOSPACE to "monospace"
        ).firstOrNull { (base, _) ->
            typeface == base || typeface == Typeface.create(base, typeface.style)
        }?.second ?: return null
        return fontCss(family, typeface.style)
    }

    private fun fontCss(family: String, style: Int): String {
        val weight = if (style and Typeface.BOLD != 0) "bold" else "normal"
        val fontStyle = if (style and Typeface.ITALIC != 0) "italic" else "normal"
        return "font-family:$family;font-weight:$weight;font-style:$fontStyle"
    }

    private fun textAlign(gravity: Int): String =
        when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> "center"
            Gravity.END -> "end"
            Gravity.RIGHT -> "right"
            Gravity.LEFT -> "left"
            else -> "start"
        }
}

package com.affirm.android

import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.affirm.android.PromoRendererMode.HTML
import com.affirm.android.PromoRendererMode.NATIVE

/** Picks HTML vs native. Order: explicit HTML, legacy opt-out, font, server flag. */
@RestrictTo(LIBRARY)
internal object PromoRendererDecision {

    const val EXPLICIT_HTML = "explicit_html"
    const val OPT_OUT = "opt_out"
    const val UNSUPPORTED_FONT = "unsupported_font"
    const val FLAG_TRUE = "flag_true"
    const val FLAG_FALSE = "flag_false"

    class Result(
        @JvmField val selected: PromoRendererMode,
        @JvmField val decisionReason: String
    )

    @JvmStatic
    fun decide(
        merchantHtmlStyling: Boolean,
        useLegacyNativeRendering: Boolean,
        fontSupported: Boolean,
        migrateToHtml: Boolean
    ): Result = when {
        merchantHtmlStyling -> Result(HTML, EXPLICIT_HTML)
        useLegacyNativeRendering -> Result(NATIVE, OPT_OUT)
        !fontSupported -> Result(NATIVE, UNSUPPORTED_FONT)
        migrateToHtml -> Result(HTML, FLAG_TRUE)
        else -> Result(NATIVE, FLAG_FALSE)
    }

    /**
     * Built-in sans-serif / serif / monospace, including bold and italic, can be shown in HTML.
     * Custom Typefaces stay native. Null is the system default font.
     */
    @JvmStatic
    fun isFontSupported(typeface: Typeface?): Boolean {
        if (typeface == null || typeface == Typeface.DEFAULT_BOLD) return true
        return listOf(Typeface.DEFAULT, Typeface.SANS_SERIF, Typeface.SERIF, Typeface.MONOSPACE)
            .any { typeface == it || typeface == Typeface.create(it, typeface.style) }
    }
}

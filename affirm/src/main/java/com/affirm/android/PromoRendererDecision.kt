package com.affirm.android

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
    const val FLAG_MISSING = "flag_missing"
    const val FLAG_NULL = "flag_null"
    const val FLAG_MALFORMED = "flag_malformed"

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
}

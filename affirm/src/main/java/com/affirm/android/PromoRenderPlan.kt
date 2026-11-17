package com.affirm.android

import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER
import com.affirm.android.PromoRendererMode.HTML
import com.affirm.android.PromoRendererMode.NATIVE
import com.affirm.android.model.PromoResponse

/** What to show first for one promos response. */
@RestrictTo(LIBRARY)
internal class PromoRenderPlan private constructor(
    @JvmField val mode: PromoRendererMode,
    /** What to show in [mode]; empty when the response has no usable copy for it. */
    @JvmField val text: String,
    /** Plain-text accessibility description: the native copy, or empty. */
    @JvmField val description: String,
    /** An HTML render that fails later may switch to [description]. */
    @JvmField val canFallBackToNative: Boolean
) {
    companion object {
        @JvmStatic
        fun create(
            merchantHtmlStyling: Boolean,
            useLegacyNativeRendering: Boolean,
            typeface: Typeface?,
            response: PromoResponse
        ): PromoRenderPlan {
            val promo = response.promo()
            val nativeText = promo.ala().replace(LOGO_PLACEHOLDER, "affirm")
                .takeIf { it.isNotBlank() }
            val htmlText = promo.htmlAla().takeIf { it.isNotBlank() }
            val migrateToHtml = promo.promoConfig().migrateNativeToHtmlRender()

            val selected = PromoRendererDecision.decide(
                merchantHtmlStyling,
                useLegacyNativeRendering,
                PromoRendererDecision.isFontSupported(typeface),
                migrateToHtml
            ).selected
            val description = nativeText.orEmpty()
            val nativeAllowed = nativeText != null && !merchantHtmlStyling
            val showHtml = selected == HTML && (htmlText != null || !nativeAllowed)

            return if (showHtml) {
                PromoRenderPlan(HTML, htmlText.orEmpty(), description, nativeAllowed)
            } else {
                PromoRenderPlan(NATIVE, description, description, false)
            }
        }
    }
}

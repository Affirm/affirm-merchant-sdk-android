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
    @JvmField val canFallBackToNative: Boolean,
    /** The renderer the decision chose; differs from [mode] when blank HTML falls back. */
    @JvmField val selected: PromoRendererMode,
    @JvmField val decisionReason: String,
    /** Set when [selected] is HTML but [mode] is native because the HTML was blank. */
    @JvmField val fallbackReason: String?,
    @JvmField val explicitHtml: Boolean,
    @JvmField val nativeUsable: Boolean,
    @JvmField val htmlPresent: Boolean
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
            return create(
                merchantHtmlStyling, useLegacyNativeRendering, typeface,
                promo.ala(), promo.htmlAla(), promo.promoConfig().migrateNativeToHtmlRender()
            )
        }

        @JvmStatic
        fun create(
            merchantHtmlStyling: Boolean,
            useLegacyNativeRendering: Boolean,
            typeface: Typeface?,
            ala: String?,
            htmlAla: String?,
            migrateToHtml: Boolean
        ): PromoRenderPlan {
            val nativeText = ala?.replace(LOGO_PLACEHOLDER, "affirm")
                ?.takeIf { it.isNotBlank() }
            val htmlText = htmlAla?.takeIf { it.isNotBlank() }

            val decision = PromoRendererDecision.decide(
                merchantHtmlStyling,
                useLegacyNativeRendering,
                PromoHtmlStyle.isFontSupported(typeface),
                migrateToHtml
            )
            val description = nativeText.orEmpty()
            val nativeAllowed = nativeText != null && !merchantHtmlStyling
            val showHtml = decision.selected == HTML && (htmlText != null || !nativeAllowed)

            val mode = if (showHtml) HTML else NATIVE
            val text = if (showHtml) htmlText.orEmpty() else description
            val fallbackReason =
                if (decision.selected == HTML && !showHtml) AffirmPromoRenderReason.MISSING_HTML
                else null
            return PromoRenderPlan(
                mode, text, description, showHtml && nativeAllowed, decision.selected,
                decision.decisionReason, fallbackReason, merchantHtmlStyling,
                nativeText != null, htmlText != null
            )
        }
    }
}

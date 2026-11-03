package com.affirm.android

import android.view.View
import com.affirm.android.exception.AffirmException

internal class PromoButtonCallback(
    private val button: AffirmPromotionButton,
    private val shownMessage: String?,
    private val shownDescription: String?
) : SpannablePromoCallback {

    @JvmField
    var request: PromoRequest? = null

    override fun onPromoWritten(promoMessage: String, promoDescription: String, showPrequal: Boolean) {
        if (!button.isCurrentPromoRequest(request)) {
            return
        }
        button.tag = showPrequal
        if (promoMessage == shownMessage && promoDescription == shownDescription) {
            return
        }
        button.setLabel(promoMessage, promoDescription)
    }

    override fun onFailure(exception: AffirmException) {
        if (!button.isCurrentPromoRequest(request)) {
            return
        }
        AffirmLog.e("Promo request failed", exception)
        if (shownMessage == null) {
            button.visibility = View.GONE
        }
    }
}

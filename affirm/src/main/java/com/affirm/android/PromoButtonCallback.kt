package com.affirm.android

import android.view.View
import com.affirm.android.exception.AffirmException

/** Writes a promo response to [button], which may already show a cached message. */
internal class PromoButtonCallback(
    private val button: AffirmPromotionButton
) : SpannablePromoCallback {

    override fun onPromoWritten(promoMessage: String, promoDescription: String, showPrequal: Boolean) {
        button.tag = showPrequal
        if (!button.hasLabel(promoMessage)) {
            button.setLabel(promoMessage, promoDescription)
        }
    }

    override fun onFailure(exception: AffirmException) {
        AffirmLog.e("Promo request failed", exception)
        if (button.isEmpty) {
            button.visibility = View.GONE
        }
    }
}

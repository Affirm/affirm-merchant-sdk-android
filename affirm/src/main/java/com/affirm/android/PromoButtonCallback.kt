package com.affirm.android

import android.view.View
import com.affirm.android.exception.AffirmException
import com.affirm.android.model.PromoResponse

/** Shows a promo response on [button], which may already show a cached message. */
internal class PromoButtonCallback(
    private val button: AffirmPromotionButton
) : PromoResponseCallback {

    override fun onPromoResponse(response: PromoResponse, showPrequal: Boolean) {
        button.showResponse(response, showPrequal)
    }

    override fun onFailure(exception: AffirmException) {
        AffirmLog.e("Promo request failed", exception)
        if (button.isEmpty) {
            button.visibility = View.GONE
        }
    }
}

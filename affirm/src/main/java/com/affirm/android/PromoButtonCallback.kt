package com.affirm.android

import com.affirm.android.exception.AffirmException
import com.affirm.android.model.PromoResponse

/** Delivers the result of [render]'s request to [button], which ignores replaced renders. */
internal class PromoButtonCallback(
    private val button: AffirmPromotionButton,
    private val render: PromoRender
) : PromoResponseCallback {

    override fun onSuccess(response: PromoResponse, showPrequal: Boolean) {
        button.onPromoResponse(render, response, showPrequal)
    }

    override fun onFailure(exception: AffirmException) {
        AffirmLog.e("Promo request failed", exception)
        button.onPromoFailure(render)
    }
}

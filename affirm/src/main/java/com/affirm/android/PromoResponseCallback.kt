package com.affirm.android

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import com.affirm.android.exception.AffirmException
import com.affirm.android.model.PromoResponse

@RestrictTo(LIBRARY)
internal interface PromoResponseCallback {
    fun onPromoResponse(response: PromoResponse, showPrequal: Boolean)

    fun onFailure(exception: AffirmException)
}

package com.affirm.android

import com.affirm.android.model.CheckoutResponse
import com.affirm.android.model.PromoResponse

interface ApiRepository {

    fun promoRequest(url: String, listener: AffirmApiRepository.AffirmApiListener<PromoResponse>)

    fun checkoutRequest(url: String, body: String, listener: AffirmApiRepository.AffirmApiListener<CheckoutResponse>)

    fun trackerRequest(url: String, body: String, listener: AffirmApiRepository.AffirmApiListener<String>)

    fun cancelRequest()
}
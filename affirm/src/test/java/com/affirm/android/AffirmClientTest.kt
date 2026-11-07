package com.affirm.android

import com.google.common.truth.Truth.assertThat
import okhttp3.Request
import org.junit.Test

class AffirmClientTest {

    private fun request(url: String) = Request.Builder().url(url).build()

    @Test
    fun promoRequestsAreRecognizedByPath() {
        assertThat(AffirmClient.isPromoRequest(
            request("https://sandbox.affirm.com/api/promos/v2/KEY?amount=1000")
        )).isTrue()
        assertThat(AffirmClient.isPromoRequest(
            request("https://sandbox.affirm.com/api/v2/checkout")
        )).isFalse()
    }
}

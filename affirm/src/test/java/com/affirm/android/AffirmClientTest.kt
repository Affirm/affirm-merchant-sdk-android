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

    @Test
    fun promoNetworkErrorLeavesOutTheUrl() {
        val data = AffirmClient.networkErrorData(request(
            "https://sandbox.uk.affirm.com/api/promos/v2/KEY?amount=110000&items=%5B%7B%22sku%22%7D%5D"
        ))!!

        assertThat(data.has("url")).isFalse()
        assertThat(data.get("method").asString).isEqualTo("GET")
    }

    @Test
    fun otherNetworkErrorsKeepTheUrl() {
        val url = "https://sandbox.affirm.com/api/v2/checkout"

        assertThat(AffirmClient.networkErrorData(request(url))!!.get("url").asString)
            .isEqualTo(url)
    }

    @Test
    fun trackerNetworkErrorsAreNotReported() {
        assertThat(AffirmClient.networkErrorData(
            request("https://tracker.affirm.com/collect")
        )).isNull()
    }
}

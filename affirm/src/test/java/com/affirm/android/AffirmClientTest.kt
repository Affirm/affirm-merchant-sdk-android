package com.affirm.android

import com.affirm.android.exception.AffirmException
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AffirmClientTest {

    @Before
    fun setUp() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            )
        }
    }

    private fun request(url: String) = Request.Builder().url(url).build()

    /** Returns whether the response body was closed. */
    private fun <T> sendAndRespond(
        listener: AffirmClient.AffirmListener<T>,
        body: String,
        code: Int = 200
    ): Boolean {
        var closed = false
        val source = object : ForwardingSource(Buffer().writeUtf8(body)) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        var callback: Callback? = null
        val call = mock(Call::class.java) { invocation ->
            when (invocation.method.name) {
                "enqueue" -> callback = invocation.getArgument(0)
                "isCanceled" -> return@mock false
                "request" -> return@mock request("https://tracker.affirm.com/collect")
            }
            null
        }
        val client = mock(OkHttpClient::class.java) { call }
        AffirmClient.send(client, object : AffirmClient.AffirmApiRequest {
            override fun url() = "https://tracker.affirm.com/collect"
            override fun method() = AffirmHttpRequest.Method.POST
            override fun body() = JsonObject()
            override fun headers(): Map<String, String>? = null
        }, listener)

        callback!!.onResponse(call, Response.Builder()
            .request(request("https://tracker.affirm.com/collect"))
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(source.buffer().asResponseBody("text/plain".toMediaType(), -1))
            .build())
        ShadowLooper.idleMainLooper()
        return closed
    }

    @Test
    fun everyResponseBodyIsClosed() {
        val void = object : AffirmClient.AffirmListener<Void> {
            override fun onSuccess(response: Void?) {}
            override fun onFailure(exception: AffirmException) {}
        }
        val json = object : AffirmClient.AffirmListener<JsonObject> {
            override fun onSuccess(response: JsonObject?) {}
            override fun onFailure(exception: AffirmException) {}
        }

        assertThat(sendAndRespond(void, "OK")).isTrue()
        assertThat(sendAndRespond(json, "{}")).isTrue()
        assertThat(sendAndRespond(json, "", code = 404)).isTrue()
    }

    @Test
    fun voidListenerSucceedsWhateverTheBody() {
        val results = mutableListOf<String>()
        sendAndRespond(object : AffirmClient.AffirmListener<Void> {
            override fun onSuccess(response: Void?) {
                results.add("success")
            }

            override fun onFailure(exception: AffirmException) {
                results.add("failure")
            }
        }, "OK")

        assertThat(results).containsExactly("success")
    }

    @Test
    fun typedListenerStillFailsOnABodyThatIsNotJson() {
        val results = mutableListOf<String>()
        sendAndRespond(object : AffirmClient.AffirmListener<JsonObject> {
            override fun onSuccess(response: JsonObject?) {
                results.add("success")
            }

            override fun onFailure(exception: AffirmException) {
                results.add("failure")
            }
        }, "OK")

        assertThat(results).containsExactly("failure")
    }

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

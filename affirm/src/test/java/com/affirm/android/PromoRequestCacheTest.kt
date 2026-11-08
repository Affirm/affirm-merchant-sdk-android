package com.affirm.android

import com.affirm.android.model.Item
import com.affirm.android.model.PromoPageType
import com.google.common.truth.Truth.assertThat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoRequestCacheTest {

    private lateinit var originalConfiguration: Affirm.Configuration

    @Before
    fun setUp() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder(PUBLIC_KEY, Affirm.Environment.SANDBOX).build()
            )
        }
        originalConfiguration = AffirmPlugins.get().configuration
        PromoMessageCache.evictAll()
    }

    @After
    fun tearDown() {
        AffirmPlugins.get().setConfiguration(originalConfiguration)
        PromoMessageCache.evictAll()
    }

    @Test
    fun successCachesBothMessagesUnderRequestKey() {
        val call = CapturingCall()
        val request = newRequest(call)

        request.create()
        respond(call, promoBody(prequal = true))

        val cached = PromoMessageCache.get(request.cacheKey())
        assertThat(cached).isNotNull()
        assertThat(cached!!.ala).isEqualTo("Starting at \$10/mo with affirm")
        assertThat(cached.htmlAla).isEqualTo("<p>Starting at \$10/mo</p>")
        assertThat(cached.showPrequal).isTrue()
    }

    @Test
    fun samePriceWithDifferentPromoDoesNotShareCache() {
        val callA = CapturingCall()
        val promoA = newRequest(callA, promoId = "promo-a")
        val promoB = newRequest(promoId = "promo-b")

        promoA.create()
        respond(callA, promoBody(prequal = true))

        assertThat(promoB.cacheKey()).isNotEqualTo(promoA.cacheKey())
        assertThat(PromoMessageCache.get(promoA.cacheKey())!!.showPrequal).isTrue()
        assertThat(PromoMessageCache.get(promoB.cacheKey())).isNull()
    }

    @Test
    fun samePriceWithDifferentPromoDoesNotShowOtherOfferOnButton() {
        val callA = CapturingCall()
        val promoA = newRequest(callA, promoId = "promo-a")
        val promoB = newRequest(promoId = "promo-b")
        promoA.create()
        respond(callA, promoBody(prequal = true))

        val buttonA = AffirmPromotionButton(RuntimeEnvironment.application)
        val buttonB = AffirmPromotionButton(RuntimeEnvironment.application)
        buttonA.showPlaceholder(PromoMessageCache.get(promoA.cacheKey()))
        buttonB.showPlaceholder(PromoMessageCache.get(promoB.cacheKey()))

        assertThat((buttonA.getChildAt(0) as PromotionButton).text.toString())
            .isEqualTo("Starting at \$10/mo with affirm")
        assertThat(buttonB.isEmpty).isTrue()
    }

    @Test
    fun failedResponseIsNotCached() {
        val call = CapturingCall()
        val request = newRequest(call)

        request.create()
        respond(call, "not json")

        assertThat(PromoMessageCache.get(request.cacheKey())).isNull()
    }

    @Test
    fun htmlAndPlainButtonsShareCacheKey() {
        assertThat(newRequest(htmlStyle = true).cacheKey())
            .isEqualTo(newRequest(htmlStyle = false).cacheKey())
    }

    @Test
    fun cacheKeyCoversEveryRequestParameter() {
        val base = newRequest()
        val variants = listOf(
            newRequest(amount = BigDecimal.TEN),
            newRequest(promoId = "promo-a"),
            newRequest(pageType = PromoPageType.CART),
            newRequest(showCta = true),
            newRequest(color = AffirmColor.AFFIRM_COLOR_TYPE_WHITE),
            newRequest(logoType = AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT),
            newRequest(items = listOf(item()))
        )

        assertThat(newRequest().cacheKey()).isEqualTo(base.cacheKey())
        variants.forEach { assertThat(it.cacheKey()).isNotEqualTo(base.cacheKey()) }
    }

    @Test
    fun responseIsCachedUnderKeyOfConfigurationItWasSentWith() {
        val call = CapturingCall()
        val request = newRequest(call)
        request.create()
        val sentKey = request.cacheKey()

        AffirmPlugins.get().setConfiguration(
            Affirm.Configuration.Builder(PUBLIC_KEY, Affirm.Environment.SANDBOX)
                .setLocale("fr_CA")
                .build()
        )
        respond(call, promoBody(prequal = false))

        assertThat(request.cacheKey()).isNotEqualTo(sentKey)
        assertThat(PromoMessageCache.get(sentKey)).isNotNull()
        assertThat(PromoMessageCache.get(request.cacheKey())).isNull()
    }

    private fun newRequest(
        call: CapturingCall = CapturingCall(),
        amount: BigDecimal = BigDecimal.ONE,
        promoId: String? = null,
        pageType: PromoPageType? = null,
        showCta: Boolean = false,
        color: AffirmColor = AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
        logoType: AffirmLogoType = AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
        items: List<Item>? = null,
        htmlStyle: Boolean = false
    ): PromoRequest {
        // Mockito's any() and captors return null, which Kotlin rejects for OkHttp's non-null
        // parameters, so the fakes answer by method name instead.
        val client = mock(OkHttpClient::class.java) { invocation ->
            if (invocation.method.name == "newCall") call.call else RETURNS_DEFAULTS.answer(invocation)
        }
        return PromoRequest(
            client, promoId, pageType, amount, showCta, color, logoType, items,
            Affirm.promoMessageCallback(htmlStyle, mock(SpannablePromoCallback::class.java))
        )
    }

    private fun respond(call: CapturingCall, body: String) {
        val response = Response.Builder()
            .request(Request.Builder().url("https://sandbox.affirm.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        call.callback.onResponse(call.call, response)
        ShadowLooper.idleMainLooper()
    }

    // showPrequal is derived from promo_style: anything but "fast" opens prequal.
    private fun promoBody(prequal: Boolean) =
        """{"promo":{"ala":"Starting at ${'$'}10/mo with {affirm_logo}",""" +
            """"html_ala":"<p>Starting at ${'$'}10/mo</p>",""" +
            """"config":{"promo_prequal_enabled":true,""" +
            """"promo_style":"${if (prequal) "simple" else "fast"}"}}}"""

    private fun item() = Item.builder()
        .setDisplayName("Wheel")
        .setImageUrl("http://merchant.com/wheel.jpg")
        .setQty(1)
        .setSku("wheel")
        .setUnitPrice(BigDecimal.ONE)
        .setUrl("http://merchant.com/wheel")
        .build()

    private class CapturingCall {
        lateinit var callback: Callback
        val call: Call = mock(Call::class.java) { invocation ->
            if (invocation.method.name == "enqueue") {
                callback = invocation.getArgument(0)
                null
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
    }

    private companion object {
        const val PUBLIC_KEY = "Y8CQXFF044903JC0"
    }
}

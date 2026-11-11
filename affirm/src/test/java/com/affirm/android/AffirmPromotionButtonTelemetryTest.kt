package com.affirm.android

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.affirm.android.AffirmTracker.TrackingEvent
import com.affirm.android.exception.APIException
import com.affirm.android.model.AffirmAdapterFactory
import com.affirm.android.model.Promo
import com.affirm.android.model.PromoConfig
import com.affirm.android.model.PromoResponse
import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowWebView
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AffirmPromotionButtonTelemetryTest {

    private val events = mutableListOf<Pair<TrackingEvent, JsonObject>>()
    private val callbacks = mutableListOf<PromoResponseCallback>()
    private val defaultSender = PromoRender.trackingSender
    private lateinit var button: AffirmPromotionButton
    private lateinit var parent: ViewGroup

    @Before
    fun setUp() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            )
        }
        PromoRender.trackingSender = { event, data -> events.add(event to data) }
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        button = AffirmPromotionButton(activity)
        activity.setContentView(button)
        parent = button.parent as ViewGroup
    }

    @After
    fun tearDown() {
        PromoRender.trackingSender = defaultSender
    }

    private fun start(cacheKey: String = "telemetry-test") {
        button.startRender { callback ->
            callbacks.add(callback)
            mock(PromoRequest::class.java).also { `when`(it.cacheKey()).thenReturn(cacheKey) }
        }
    }

    private fun respond(ala: String = NATIVE, htmlAla: String = HTML, migrate: Boolean = false) {
        callbacks.last().onSuccess(response(ala, htmlAla, migrate), false)
    }

    private fun useButtonWithoutWebView() {
        val activity = button.context as Activity
        button = object : AffirmPromotionButton(activity) {
            override fun createPromotionWebView(): PromotionWebView =
                throw IllegalStateException("No WebView provider")
        }
        activity.setContentView(button)
        parent = button.parent as ViewGroup
    }

    private fun names() = events.map { it.first }

    private fun results() =
        events.filter { it.first == TrackingEvent.PROMO_RENDER_RESULT }.map { it.second }

    private fun JsonObject.string(name: String): String? =
        get(name).takeUnless { it.isJsonNull }?.asString

    private fun message(webView: PromotionWebView, body: String) {
        webView.onScriptMessage("{\"renderId\":\"${webView.renderId()}\",$body}")
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun nativeResponseEndsTheRender() {
        start()
        assertThat(events).isEmpty()

        respond()

        assertThat(names()).containsExactly(
            TrackingEvent.PROMO_RENDER_MODE, TrackingEvent.PROMO_RENDER_RESULT
        ).inOrder()
        val result = results().single()
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
    }

    @Test
    fun responseSetsPrequal() {
        start()
        callbacks.last().onSuccess(response(NATIVE, HTML, false), true)

        assertThat(button.showsPrequal()).isTrue()
    }

    @Test
    fun htmlEndsTheRenderWhenItShows() {
        start()
        respond(migrate = true)
        val webView = button.promotionWebView!!
        message(webView, "\"event\":\"sanitized\",\"usable\":true")
        assertThat(names()).containsExactly(TrackingEvent.PROMO_RENDER_MODE)

        message(webView, "\"event\":\"ready\",\"height\":20")

        val result = results().single()
        assertThat(result.string("actual_renderer")).isEqualTo("html")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.get("html_content_usable").asBoolean).isTrue()
    }

    @Test
    fun htmlTimeoutFallsBackToNative() {
        start()
        respond(migrate = true)

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        val result = results().single()
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)
        assertThat(result.get("fallback_succeeded").asBoolean).isTrue()
        assertThat(result.get("html_content_usable").isJsonNull).isTrue()
    }

    @Test
    fun newFallbackCopyUpdatesTheReusedFallback() {
        PromoMessageCache.put(
            "telemetry-fallback", PromoMessageCache.CachedMessage(NATIVE, HTML, false, true)
        )
        start("telemetry-fallback")
        respond(migrate = true)
        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        start("telemetry-fallback")
        respond(ala = "Pay in 4 with affirm", migrate = true)

        assertThat((button.getChildAt(0) as PromotionButton).text.toString())
            .isEqualTo("Pay in 4 with affirm")
        val result = results().last()
        assertThat(results()).hasSize(2)
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("fallback_reason")).isNull()
    }

    @Test
    fun explicitHtmlFailureHidesWithoutFallback() {
        button.configWithHtmlStyling()
        start()
        respond()

        message(button.promotionWebView!!, "\"event\":\"failure\",\"reason\":\"resource_failed\"")

        val result = results().single()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("actual_renderer")).isEqualTo("none")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.RESOURCE_FAILED)
        assertThat(result.string("fallback_reason")).isNull()
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun htmlFailureWithBlankNativeTextHides() {
        start()
        respond(ala = "", migrate = true)

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        val result = results().single()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("actual_renderer")).isEqualTo("none")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)
        assertThat(result.string("fallback_reason")).isNull()
        assertThat(result.get("native_content_usable").asBoolean).isFalse()
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun nativeErrorEndsTheRender() {
        breakNativeRendering()
        start()
        respond()

        val result = results().single()
        assertThat(result.string("selected_mode")).isEqualTo("native")
        assertThat(result.string("actual_renderer")).isEqualTo("none")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.NATIVE_ERROR)
        assertThat(result.string("fallback_reason")).isNull()
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun nativeErrorDuringFallbackKeepsTheHtmlReason() {
        breakNativeRendering()
        start()
        respond(migrate = true)

        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)

        val result = results().single()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.NATIVE_ERROR)
        assertThat(result.string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)
        assertThat(result.get("fallback_succeeded").asBoolean).isFalse()
    }

    /** A null logo color makes the native logo lookup throw. */
    private fun breakNativeRendering() {
        AffirmPromotionButton::class.java
            .getMethod("setAffirmColor", AffirmColor::class.java)
            .invoke(button, null)
    }

    @Test
    fun reboundFallbackIsNotCountedAgain() {
        PromoMessageCache.put(
            "telemetry-rebound", PromoMessageCache.CachedMessage(NATIVE, HTML, false, true)
        )
        start("telemetry-rebound")
        respond(migrate = true)
        ShadowLooper.idleMainLooper(AffirmPromoRendering.HTML_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        assertThat(results().single().string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)

        start("telemetry-rebound")
        respond(migrate = true)

        val rebound = results().last()
        assertThat(results()).hasSize(2)
        assertThat(rebound.string("selected_mode")).isEqualTo("html")
        assertThat(rebound.string("actual_renderer")).isEqualTo("native")
        assertThat(rebound.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(rebound.string("fallback_reason")).isNull()
        assertThat(rebound.get("fallback_succeeded").asBoolean).isFalse()
        assertThat(button.getChildAt(0)).isInstanceOf(PromotionButton::class.java)
    }

    @Test
    fun emptyResponseIsEmptyContent() {
        start()
        respond(ala = "", htmlAla = "")

        val result = results().single()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.EMPTY_CONTENT)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.MISSING_NATIVE)
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun fetchFailureEndsTheRender() {
        start()
        callbacks.last().onFailure(APIException("boom", null))

        val result = results().single()
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.FETCH_ERROR)
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun fetchFailureKeepsTheCachedMessage() {
        PromoMessageCache.put(
            "telemetry-cached", PromoMessageCache.CachedMessage(NATIVE, HTML, false, false)
        )
        start("telemetry-cached")
        callbacks.last().onFailure(APIException("boom", null))

        assertThat(button.visibility).isEqualTo(View.VISIBLE)
        assertThat(button.getChildAt(0)).isInstanceOf(PromotionButton::class.java)
    }

    @Test
    fun finishingHostEndsTheRender() {
        (button.context as Activity).finish()
        start()

        assertThat(names()).containsExactly(
            TrackingEvent.PROMO_RENDER_MODE, TrackingEvent.PROMO_RENDER_RESULT
        ).inOrder()
        assertThat(results().single().string("reason"))
            .isEqualTo(AffirmPromoRenderReason.HOST_DESTROYED)
    }

    @Test
    fun reconfigureSupersedesTheRunningRender() {
        start()
        start()

        val cancelled = results().single()
        assertThat(cancelled.string("reason")).isEqualTo(AffirmPromoRenderReason.SUPERSEDED)

        callbacks.first().onSuccess(response(NATIVE, HTML, false), false)
        assertThat(results()).hasSize(1)
        assertThat(button.isEmpty).isTrue()
    }

    @Test
    fun decisionUsesTheConfigurationAtStart() {
        start()
        button.configWithHtmlStyling()
        respond()

        val mode = events.first().second
        assertThat(mode.string("requested_mode")).isEqualTo("native")
        assertThat(button.getChildAt(0)).isInstanceOf(PromotionButton::class.java)
    }

    @Test
    fun optOutSetAfterInitializationAppliesFromTheNextRender() {
        start()
        Affirm.setUseLegacyNativeRendering(true)
        try {
            respond(migrate = true)
            start()
            respond(migrate = true)
        } finally {
            Affirm.setUseLegacyNativeRendering(false)
        }

        val reasons = events.filter { it.first == TrackingEvent.PROMO_RENDER_MODE }
            .map { it.second.string("decision_reason") }
        assertThat(reasons).containsExactly(
            PromoRendererDecision.FLAG_TRUE, PromoRendererDecision.OPT_OUT
        ).inOrder()
        assertThat(button.getChildAt(0)).isInstanceOf(PromotionButton::class.java)
    }

    @Test
    fun migratedHtmlUsesTheStyleAtStart() {
        start()
        val pageAtStart = PromoHtmlStyle.migratedCss(
            button.nativeTypeface(), Color.BLACK,
            button.resources.getDimensionPixelSize(R.dimen.affirm_promotion_size).toFloat(),
            Gravity.TOP or Gravity.START, button.resources.displayMetrics.density
        )!!
        button.configWithLocalStyling(
            AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
            Typeface.SERIF, android.R.color.white
        )
        respond(migrate = true)

        val page = Shadow.extract<ShadowWebView>(button.promotionWebView!!)
            .lastLoadDataWithBaseURL.data
        assertThat(page).contains(AffirmPromoRendering.fontDeclaration(pageAtStart).css)
    }

    @Test
    fun detachCancelsAndReattachStartsAgain() {
        start()
        parent.removeView(button)
        assertThat(results()).isEmpty()
        ShadowLooper.idleMainLooper()

        val cancelled = results().single()
        assertThat(cancelled.string("reason")).isEqualTo(AffirmPromoRenderReason.DETACHED)
        assertThat(callbacks).hasSize(1)

        parent.addView(button)
        assertThat(callbacks).hasSize(2)
        respond()

        val restarted = results().last()
        assertThat(restarted.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(restarted.string("render_id")).isNotEqualTo(cancelled.string("render_id"))
    }

    @Test
    fun renderThatEndsBeforeThePendingDetachIsKept() {
        start()
        parent.removeView(button)
        respond()
        ShadowLooper.idleMainLooper()

        val result = results().single()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        parent.addView(button)
        assertThat(callbacks).hasSize(1)
    }

    @Test
    fun finishedRenderSurvivesDetach() {
        start()
        respond()
        val shown = button.getChildAt(0)

        parent.removeView(button)
        parent.addView(button)

        assertThat(events).hasSize(2)
        assertThat(callbacks).hasSize(1)
        assertThat(button.getChildAt(0)).isSameAs(shown)
    }

    @Test
    fun reconfigureWhileDetachedReplacesTheRestart() {
        start()
        parent.removeView(button)
        ShadowLooper.idleMainLooper()
        start()

        parent.addView(button)

        assertThat(callbacks).hasSize(2)
    }

    @Test
    fun pendingDetachDoesNotCancelANewerRender() {
        start()
        parent.removeView(button)
        start()
        ShadowLooper.idleMainLooper()

        assertThat(results().single().string("reason"))
            .isEqualTo(AffirmPromoRenderReason.SUPERSEDED)
        parent.addView(button)
        assertThat(callbacks).hasSize(2)
    }

    @Test
    fun moveWithinOnePassKeepsTheRender() {
        start()
        parent.removeView(button)
        parent.addView(button)
        ShadowLooper.idleMainLooper()

        assertThat(events).isEmpty()
        respond()
        assertThat(results().single().string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(callbacks).hasSize(1)
    }

    @Test
    fun destroyRightAfterDetachIsHostDestroyed() {
        start()
        parent.removeView(button)
        button.destroy()
        ShadowLooper.idleMainLooper()

        assertThat(results().single().string("reason"))
            .isEqualTo(AffirmPromoRenderReason.HOST_DESTROYED)
    }

    @Test
    fun missingWebViewFallsBackToNative() {
        useButtonWithoutWebView()
        start()
        respond(migrate = true)

        val result = results().single()
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.WEBVIEW_UNAVAILABLE)
        assertThat(result.get("fallback_succeeded").asBoolean).isTrue()
        assertThat(button.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun missingWebViewHidesExplicitHtml() {
        useButtonWithoutWebView()
        button.configWithHtmlStyling()
        start()
        respond()

        val result = results().single()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.WEBVIEW_UNAVAILABLE)
        assertThat(button.visibility).isEqualTo(View.GONE)
    }

    @Test
    fun failureOfAReplacedRequestIsIgnored() {
        start()
        start()
        assertThat(button.visibility).isEqualTo(View.VISIBLE)

        callbacks.first().onFailure(APIException("boom", null))
        assertThat(results()).hasSize(1)
        assertThat(button.visibility).isEqualTo(View.VISIBLE)

        respond()
        assertThat(results()).hasSize(2)
        assertThat(results().last().string("outcome")).isEqualTo(PromoRender.SUCCESS)
    }

    @Test
    fun messagesFromAReplacedRenderAreIgnored() {
        start()
        respond(migrate = true)
        val webView = button.promotionWebView!!
        val first = webView.renderId()
        start()
        respond(htmlAla = "$HTML<br>", migrate = true)
        assertThat(button.promotionWebView).isSameAs(webView)

        for (body in listOf("\"event\":\"sanitized\",\"usable\":false", "\"event\":\"ready\"")) {
            webView.onScriptMessage("{\"renderId\":\"$first\",$body}")
            ShadowLooper.idleMainLooper()
        }
        assertThat(results()).hasSize(1)

        message(webView, "\"event\":\"sanitized\",\"usable\":true")
        message(webView, "\"event\":\"ready\",\"height\":20")
        val result = results().last()
        assertThat(result.string("actual_renderer")).isEqualTo("html")
        assertThat(result.get("html_content_usable").asBoolean).isTrue()
    }

    @Test
    fun configureWithAmountRunsAManagedRender() {
        (button.context as Activity).finish()

        Affirm.configureWithAmount(button, BigDecimal("1100"), false)

        assertThat(names()).containsExactly(
            TrackingEvent.PROMO_RENDER_MODE, TrackingEvent.PROMO_RENDER_RESULT
        ).inOrder()
        assertThat(results().single().string("reason"))
            .isEqualTo(AffirmPromoRenderReason.HOST_DESTROYED)
    }

    @Test
    fun realResponseWithoutTheFlagReportsItMissing() {
        start()
        val response = GsonBuilder()
            .registerTypeAdapterFactory(AffirmAdapterFactory.create())
            .create()
            .fromJson(REAL_RESPONSE, PromoResponse::class.java)
        callbacks.last().onSuccess(response, false)

        val mode = events.first { it.first == TrackingEvent.PROMO_RENDER_MODE }.second
        assertThat(mode.string("selected_mode")).isEqualTo("native")
        assertThat(mode.string("flag_status")).isEqualTo(PromoConfig.FLAG_MISSING)
        assertThat(mode.get("flag_value").asBoolean).isFalse()
        assertThat(mode.string("decision_reason")).isEqualTo(PromoRendererDecision.FLAG_MISSING)
        val result = results().single()
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.get("native_content_usable").asBoolean).isTrue()
        assertThat(result.get("html_content_present").asBoolean).isTrue()
    }

    @Test
    fun destroyCancelsWithoutRestart() {
        start()
        button.destroy()

        assertThat(results().single().string("reason"))
            .isEqualTo(AffirmPromoRenderReason.HOST_DESTROYED)

        parent.removeView(button)
        parent.addView(button)
        assertThat(callbacks).hasSize(1)
        assertThat(results()).hasSize(1)
    }

    private companion object {
        const val NATIVE = "Pay over time with affirm"
        const val HTML = "<span>Pay over time</span>"

        // A sandbox promos response body, as returned. It has no migration flag.
        const val REAL_RESPONSE = """{"promo":{"ala":"0% APR or as low as ${'$'}54/mo with {affirm_logo}. Learn more","html_ala":"<span class='__apr_badge __apr_badge_indigo'>0% APR </span>or as low as <span class='affirm-ala-price'>${'$'}54</span>/mo with <span class='__affirm-logo __affirm-logo-blue'>Affirm</span>. <a class='affirm-modal-trigger'>Learn more</a>","config":{"promo_prequal_enabled":false,"promo_style":"adaptive","merchant_ari":"C8QU0QPNAD4VRGK3","loan_type":"installment","user_ari":"","toast_enabled":false,"enabled_integrations":[]}}}"""

        fun response(ala: String, htmlAla: String, migrate: Boolean): PromoResponse =
            PromoResponse.builder()
                .setPromo(
                    Promo.builder()
                        .setAla(ala)
                        .setHtmlAla(htmlAla)
                        .setPromoConfig(
                            PromoConfig.builder()
                                .setPromoPrequalEnabled(false)
                                .setMigrateNativeToHtmlRenderValue(migrate)
                                .build()
                        )
                        .build()
                )
                .build()
    }
}

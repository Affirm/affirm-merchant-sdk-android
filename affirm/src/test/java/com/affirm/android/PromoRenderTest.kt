package com.affirm.android

import com.affirm.android.AffirmTracker.TrackingEvent
import com.affirm.android.PromoRender.Outcome
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PromoRenderTest {

    private val events = mutableListOf<Pair<TrackingEvent, JsonObject>>()
    private var now = 100L

    private fun render(explicitHtml: Boolean = false, optOut: Boolean = false) = PromoRender(
        explicitHtml, optOut, null, "KEY", "sandbox", { now }
    ) { event, data -> events.add(event to data) }

    private fun plan(
        ala: String = NATIVE,
        htmlAla: String = HTML,
        migrate: Boolean = false,
        explicitHtml: Boolean = false
    ) = PromoRenderPlan.create(explicitHtml, false, null, ala, htmlAla, migrate)

    private fun mode(): JsonObject {
        assertThat(events.map { it.first }).contains(TrackingEvent.PROMO_RENDER_MODE)
        return events.first { it.first == TrackingEvent.PROMO_RENDER_MODE }.second
    }

    private fun result(): JsonObject {
        assertThat(events.map { it.first })
            .containsExactly(TrackingEvent.PROMO_RENDER_MODE, TrackingEvent.PROMO_RENDER_RESULT)
            .inOrder()
        return events[1].second
    }

    private fun JsonObject.string(name: String): String? =
        get(name).takeUnless { it.isJsonNull }?.asString

    @Test
    fun nativeSuccess() {
        val render = render()
        now = 150
        render.onResponse(plan(), "false")
        now = 170
        render.onSettled(Outcome.shown(PromoRendererMode.NATIVE, null), null)

        val mode = mode()
        assertThat(mode.string("requested_mode")).isEqualTo("native")
        assertThat(mode.string("selected_mode")).isEqualTo("native")
        assertThat(mode.get("flag_value").asBoolean).isFalse()
        assertThat(mode.string("flag_status")).isEqualTo("false")
        assertThat(mode.get("opt_out").asBoolean).isFalse()
        assertThat(mode.string("decision_reason")).isEqualTo(plan().decisionReason)

        val result = result()
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.string("reason")).isNull()
        assertThat(result.string("fallback_reason")).isNull()
        assertThat(result.get("fallback_succeeded").asBoolean).isFalse()
        assertThat(result.string("fetch_outcome")).isEqualTo("usable")
        assertThat(result.get("native_content_usable").asBoolean).isTrue()
        assertThat(result.get("html_content_present").asBoolean).isTrue()
        assertThat(result.get("html_content_usable").isJsonNull).isTrue()
        assertThat(result.get("fetch_duration_ms").asLong).isEqualTo(50)
        assertThat(result.get("render_duration_ms").asLong).isEqualTo(20)
        assertThat(result.get("total_duration_ms").asLong).isEqualTo(70)
        assertThat(result.string("server_request_id_status")).isEqualTo("unverified")
    }

    @Test
    fun bothEventsShareTheRenderIdentity() {
        val render = render()
        render.onResponse(plan(), "false")
        render.onSettled(Outcome.shown(PromoRendererMode.NATIVE, null), null)

        val mode = mode()
        val result = result()
        for (field in listOf("render_id", "request_attempt_id", "selected_mode")) {
            assertThat(result.string(field)).isEqualTo(mode.string(field))
        }
        for (data in listOf(mode, result)) {
            assertThat(data.string("render_id")).isNotEmpty()
            assertThat(data.string("merchant_key")).isEqualTo("KEY")
            assertThat(data.string("environment")).isEqualTo("sandbox")
            assertThat(data.string("platform")).isEqualTo("android")
        }
    }

    @Test
    fun eachRenderHasItsOwnId() {
        render().cancel(AffirmPromoRenderReason.SUPERSEDED)
        render().cancel(AffirmPromoRenderReason.SUPERSEDED)

        val ids = events.map { it.second.string("render_id") }.distinct()
        assertThat(ids).hasSize(2)
    }

    @Test
    fun htmlSuccess() {
        val render = render()
        render.onResponse(plan(migrate = true), "true")
        render.onSettled(Outcome.shown(PromoRendererMode.HTML, null), true)

        assertThat(mode().string("selected_mode")).isEqualTo("html")
        assertThat(mode().get("flag_value").asBoolean).isTrue()
        val result = result()
        assertThat(result.string("actual_renderer")).isEqualTo("html")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.get("html_content_usable").asBoolean).isTrue()
    }

    @Test
    fun nativeFallbackAfterHtmlFailure() {
        val render = render()
        render.onResponse(plan(migrate = true), "true")
        render.onSettled(
            Outcome.shown(PromoRendererMode.NATIVE, AffirmPromoRenderReason.HTML_TIMEOUT), false
        )

        val result = result()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("actual_renderer")).isEqualTo("native")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.SUCCESS)
        assertThat(result.string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)
        assertThat(result.get("fallback_succeeded").asBoolean).isTrue()
        assertThat(result.get("html_content_usable").asBoolean).isFalse()
    }

    @Test
    fun fetchFailureEmitsANoneDecisionFirst() {
        val render = render()
        now = 400
        render.onFetchFailed()

        val mode = mode()
        assertThat(mode.string("selected_mode")).isEqualTo("none")
        assertThat(mode.string("flag_status")).isEqualTo(PromoRender.FLAG_NOT_RECEIVED)
        assertThat(mode.string("decision_reason")).isEqualTo(AffirmPromoRenderReason.FETCH_ERROR)
        val result = result()
        assertThat(result.string("actual_renderer")).isEqualTo("none")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.FETCH_ERROR)
        assertThat(result.string("fetch_outcome")).isEqualTo("error")
        assertThat(result.get("native_content_usable").asBoolean).isFalse()
        assertThat(result.get("fetch_duration_ms").asLong).isEqualTo(300)
        assertThat(result.get("render_duration_ms").asLong).isEqualTo(0)
    }

    @Test
    fun explicitHtmlFailure() {
        val render = render(explicitHtml = true)
        render.onResponse(plan(explicitHtml = true), "false")
        render.onSettled(Outcome.failed(AffirmPromoRenderReason.HTML_TIMEOUT, null), null)

        assertThat(mode().string("requested_mode")).isEqualTo("html")
        val result = result()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("actual_renderer")).isEqualTo("none")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.HTML_TIMEOUT)
        assertThat(result.string("fallback_reason")).isNull()
        assertThat(result.get("fallback_succeeded").asBoolean).isFalse()
    }

    @Test
    fun emptyContent() {
        val render = render()
        render.onResponse(plan(ala = "", htmlAla = ""), "missing")
        render.onSettled(Outcome.empty(AffirmPromoRenderReason.MISSING_NATIVE), null)

        assertThat(mode().string("flag_status")).isEqualTo("missing")
        val result = result()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.EMPTY_CONTENT)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.MISSING_NATIVE)
        assertThat(result.string("fetch_outcome")).isEqualTo("empty")
    }

    @Test
    fun nativeErrorDuringFallbackKeepsTheHtmlReason() {
        val render = render()
        render.onResponse(plan(migrate = true), "true")
        render.onSettled(
            Outcome.failed(
                AffirmPromoRenderReason.NATIVE_ERROR, AffirmPromoRenderReason.RENDER_PROCESS_GONE
            ),
            null
        )

        val result = result()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.FAILURE)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.NATIVE_ERROR)
        assertThat(result.string("fallback_reason"))
            .isEqualTo(AffirmPromoRenderReason.RENDER_PROCESS_GONE)
        assertThat(result.get("fallback_succeeded").asBoolean).isFalse()
    }

    @Test
    fun cancelBeforeTheResponse() {
        render().cancel(AffirmPromoRenderReason.DETACHED)

        val mode = mode()
        assertThat(mode.string("selected_mode")).isEqualTo("none")
        assertThat(mode.string("decision_reason")).isEqualTo(AffirmPromoRenderReason.DETACHED)
        val result = result()
        assertThat(result.string("outcome")).isEqualTo(PromoRender.CANCELLED)
        assertThat(result.string("reason")).isEqualTo(AffirmPromoRenderReason.DETACHED)
        assertThat(result.string("fetch_outcome")).isEqualTo("cancelled")
    }

    @Test
    fun cancelWhileHtmlLoads() {
        val render = render()
        render.onResponse(plan(migrate = true), "true")
        render.cancel(AffirmPromoRenderReason.SUPERSEDED)

        val result = result()
        assertThat(result.string("selected_mode")).isEqualTo("html")
        assertThat(result.string("outcome")).isEqualTo(PromoRender.CANCELLED)
        assertThat(result.string("fetch_outcome")).isEqualTo("usable")
    }

    @Test
    fun endsOnlyOnce() {
        val render = render()
        render.onResponse(plan(), "false")
        render.onResponse(plan(), "false")
        render.onSettled(Outcome.shown(PromoRendererMode.NATIVE, null), null)
        render.onSettled(Outcome.failed(AffirmPromoRenderReason.NATIVE_ERROR, null), null)
        render.cancel(AffirmPromoRenderReason.HOST_DESTROYED)
        render.onFetchFailed()

        assertThat(render.isEnded).isTrue()
        assertThat(result().string("outcome")).isEqualTo(PromoRender.SUCCESS)
    }

    @Test
    fun contentShownBeforeTheResponseDoesNotEndTheRender() {
        val render = render()
        render.onSettled(Outcome.shown(PromoRendererMode.NATIVE, null), null)

        assertThat(render.isEnded).isFalse()
        assertThat(events).isEmpty()
    }

    @Test
    fun trackingFailureDoesNotThrow() {
        val render = PromoRender(false, false, null, "KEY", "sandbox", { 0 }) { _, _ ->
            throw IllegalStateException("boom")
        }

        render.onResponse(plan(), "false")
        render.onSettled(Outcome.shown(PromoRendererMode.NATIVE, null), null)

        assertThat(render.isEnded).isTrue()
    }

    private companion object {
        const val NATIVE = "Pay over time with affirm"
        const val HTML = "<span>Pay over time</span>"
    }
}

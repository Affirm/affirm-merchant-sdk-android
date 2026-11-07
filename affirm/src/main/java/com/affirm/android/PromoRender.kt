package com.affirm.android

import android.graphics.Typeface
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import com.affirm.android.AffirmTracker.TrackingEvent
import com.affirm.android.model.PromoConfig
import com.google.gson.JsonObject
import java.util.Locale
import java.util.UUID

/**
 * One render of a managed promo view: from `configureWithAmount` (or a restart after reattach)
 * to exactly one ending. Holds the decision inputs captured at the start, and emits
 * `Promo_render_mode` and `Promo_render_result` once each.
 */
@MainThread
@RestrictTo(LIBRARY)
internal class PromoRender @VisibleForTesting constructor(
    @JvmField val explicitHtml: Boolean,
    @JvmField val optOut: Boolean,
    @JvmField val typeface: Typeface?,
    private val merchantKey: String,
    private val environment: String,
    private val clock: () -> Long,
    private val sender: (TrackingEvent, JsonObject) -> Unit
) {

    /** How a render ended, apart from what the response said. */
    class Outcome private constructor(
        @JvmField val actual: PromoRendererMode?,
        @JvmField val outcome: String,
        @JvmField val reason: String?,
        @JvmField val fallbackReason: String?
    ) {
        companion object {
            /** [fallbackReason] is set when native is shown after HTML failed. */
            @JvmStatic
            fun shown(actual: PromoRendererMode, fallbackReason: String?) =
                Outcome(actual, SUCCESS, null, fallbackReason)

            /** [fallbackReason] is set when the native fallback itself failed. */
            @JvmStatic
            fun failed(reason: String, fallbackReason: String?) =
                Outcome(null, FAILURE, reason, fallbackReason)

            @JvmStatic
            fun empty(reason: String) = Outcome(null, EMPTY_CONTENT, reason, null)

            fun cancelled(reason: String) = Outcome(null, CANCELLED, reason, null)
        }
    }

    private val renderId = UUID.randomUUID().toString()
    private val requestAttemptId = UUID.randomUUID().toString()
    private val startedAt = clock()
    private var fetchEndedAt = -1L
    private var plan: PromoRenderPlan? = null
    private var flagStatus = FLAG_NOT_RECEIVED

    var isEnded = false
        private set

    /** Emits the decision for [plan]. Later calls are ignored. */
    fun onResponse(plan: PromoRenderPlan, flagStatus: String) {
        if (isEnded || this.plan != null) return
        fetchEndedAt = clock()
        this.plan = plan
        this.flagStatus = flagStatus
        emitMode(plan.selected.code, plan.decisionReason)
    }

    /** Ends the render with what the view shows, once the response has been decided. */
    fun onSettled(outcome: Outcome, htmlContentUsable: Boolean?) {
        if (isEnded || plan == null) return
        end(outcome, htmlContentUsable)
    }

    fun onFetchFailed() {
        if (isEnded || plan != null) return
        fetchEndedAt = clock()
        emitMode(NONE, AffirmPromoRenderReason.FETCH_ERROR)
        end(Outcome.failed(AffirmPromoRenderReason.FETCH_ERROR, null), null)
    }

    /** [reason] is superseded, detached or host_destroyed. */
    fun cancel(reason: String) {
        if (isEnded) return
        if (plan == null) {
            fetchEndedAt = clock()
            emitMode(NONE, reason)
        }
        end(Outcome.cancelled(reason), null)
    }

    private fun end(outcome: Outcome, htmlContentUsable: Boolean?) {
        isEnded = true
        val endedAt = clock()
        val plan = plan
        val data = common(plan?.selected?.code ?: NONE).apply {
            addProperty("actual_renderer", outcome.actual?.code ?: NONE)
            addProperty("outcome", outcome.outcome)
            addProperty("reason", outcome.reason)
            addProperty("fallback_reason", outcome.fallbackReason)
            addProperty("fallback_succeeded",
                outcome.actual == PromoRendererMode.NATIVE && outcome.fallbackReason != null)
            addProperty("fetch_outcome", fetchOutcome(plan, outcome))
            addProperty("native_content_usable", plan?.nativeUsable ?: false)
            addProperty("html_content_present", plan?.htmlPresent ?: false)
            addProperty("html_content_usable", htmlContentUsable)
            addProperty("fetch_duration_ms", fetchEndedAt - startedAt)
            addProperty("render_duration_ms", if (plan == null) 0 else endedAt - fetchEndedAt)
            addProperty("total_duration_ms", endedAt - startedAt)
            addProperty("server_request_id_status", "unverified")
        }
        send(TrackingEvent.PROMO_RENDER_RESULT, data)
    }

    private fun emitMode(selected: String, decisionReason: String) {
        val data = common(selected).apply {
            addProperty("requested_mode", if (explicitHtml) HTML else NATIVE)
            addProperty("flag_value", flagStatus == PromoConfig.FLAG_TRUE)
            addProperty("flag_status", flagStatus)
            addProperty("opt_out", optOut)
            addProperty("decision_reason", decisionReason)
        }
        send(TrackingEvent.PROMO_RENDER_MODE, data)
    }

    private fun common(selected: String) = JsonObject().apply {
        addProperty("render_id", renderId)
        addProperty("request_attempt_id", requestAttemptId)
        addProperty("merchant_key", merchantKey)
        addProperty("environment", environment)
        addProperty("platform", "android")
        addProperty("selected_mode", selected)
    }

    private fun send(event: TrackingEvent, data: JsonObject) {
        try {
            sender(event, data)
        } catch (e: Exception) {
            // Tracking must never affect rendering.
            AffirmLog.w("Failed to track ${event.name}: $e")
        }
    }

    companion object {
        const val SUCCESS = "success"
        const val FAILURE = "failure"
        const val EMPTY_CONTENT = "empty_content"
        const val CANCELLED = "cancelled"

        private const val NATIVE = "native"
        private const val HTML = "html"
        private const val NONE = "none"
        const val FLAG_NOT_RECEIVED = "not_received"

        private val PromoRendererMode.code: String
            get() = if (this == PromoRendererMode.HTML) HTML else NATIVE

        private fun fetchOutcome(plan: PromoRenderPlan?, outcome: Outcome): String = when {
            plan == null && outcome.outcome == CANCELLED -> "cancelled"
            plan == null -> "error"
            plan.nativeUsable || plan.htmlPresent -> "usable"
            else -> "empty"
        }

        @VisibleForTesting
        @JvmStatic
        var trackingSender: (TrackingEvent, JsonObject) -> Unit = { event, data ->
            AffirmTracker.track(event, AffirmTracker.TrackingLevel.INFO, data)
        }

        /** Starts a render with the current SDK configuration. */
        @JvmStatic
        fun start(explicitHtml: Boolean, optOut: Boolean, typeface: Typeface?): PromoRender {
            val plugins = AffirmPlugins.get()
            return PromoRender(
                explicitHtml, optOut, typeface,
                plugins?.publicKey().orEmpty(),
                plugins?.environmentName()?.lowercase(Locale.ROOT).orEmpty(),
                SystemClock::elapsedRealtime,
                trackingSender
            )
        }
    }
}

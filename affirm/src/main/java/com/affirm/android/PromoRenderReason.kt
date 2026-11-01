package com.affirm.android

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY

@RestrictTo(LIBRARY)
internal object PromoRenderReason {
    const val LOAD_ERROR = "load_error"
    const val RENDER_PROCESS_GONE = "render_process_gone"
    const val SCRIPT_ERROR = "script_error"
    const val SANITIZER_UNAVAILABLE = "sanitizer_unavailable"
    const val HTML_EMPTY = "html_empty"
    const val RESOURCE_FAILED = "resource_failed"
    const val IMAGE_FAILED = "image_failed"
    const val HTML_TIMEOUT = "html_timeout"

    private val PAGE_REASONS = setOf(
        SANITIZER_UNAVAILABLE, HTML_EMPTY, RESOURCE_FAILED, IMAGE_FAILED, HTML_TIMEOUT
    )

    /** Bridge calls can pass anything; unknown codes become script_error. */
    @JvmStatic
    fun fromPage(reason: String?): String = reason?.takeIf { it in PAGE_REASONS } ?: SCRIPT_ERROR
}

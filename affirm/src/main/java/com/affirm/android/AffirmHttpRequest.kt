package com.affirm.android

import android.webkit.CookieManager
import java.io.OutputStream
import java.io.UnsupportedEncodingException

internal open class AffirmHttpRequest internal constructor(
    val method: Method,
    val url: String,
    private val body: String? = null
) {
    internal val headers: Map<String, String>
        get() {
            val cookieManager = CookieManager.getInstance()
            val cookie = cookieManager
                .getCookie(AffirmConstants.HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl())
            return mapOf(
                "Accept" to "application/json",
                "Content-Type" to "application/json",
                "Affirm-User-Agent" to "Affirm-Android-SDK",
                "Affirm-User-Agent-Version" to BuildConfig.VERSION_NAME
            )
                .plus(
                    cookie?.let {
                        mapOf("Cookie" to it)
                    }.orEmpty()
                )
        }

    internal open fun writeBody(outputStream: OutputStream) {
        try {
            body?.toByteArray(Charsets.UTF_8).let {
                outputStream.write(it)
                outputStream.flush()
            }
        } catch (e: UnsupportedEncodingException) {
            throw e
        }
    }

    override fun toString(): String {
        return "${method.code} $url"
    }

    internal enum class Method(val code: String) {
        GET("GET"),
        POST("POST")
    }

    internal companion object {
        fun createGet(url: String): AffirmHttpRequest {
            return AffirmHttpRequest(Method.GET, url, null)
        }

        fun createPost(url: String, body: String): AffirmHttpRequest {
            return AffirmHttpRequest(Method.POST, url, body)
        }
    }
}

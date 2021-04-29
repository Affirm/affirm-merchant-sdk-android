package com.affirm.android

import com.affirm.android.AffirmConstants.X_AFFIRM_REQUEST_ID
import com.affirm.android.exception.APIException
import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws

internal data class AffirmHttpResponse internal constructor(
    val code: Int,
    val body: String?,
    val headers: Map<String, List<String>> = emptyMap()
) {
    internal val isError: Boolean = code < 200 || code >= 300

    internal val responseJson: JSONObject
        @Throws(APIException::class)
        get() {
            return body?.let {
                try {
                    JSONObject(it)
                } catch (e: JSONException) {
                    throw APIException(
                        "Exception while parsing response body. Status code: $code Request-Id: $requestId",
                        e
                    )
                }
            } ?: JSONObject()
        }

    val requestId: String? = getHeaderValue(X_AFFIRM_REQUEST_ID)

    fun getHeaderValue(key: String): String? {
        return headers.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.firstOrNull()
    }

    override fun toString(): String {
        return "Status Code: $code, Request-Id: $requestId"
    }
}

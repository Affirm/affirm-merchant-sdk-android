package com.affirm.android

import com.affirm.android.exception.APIException
import com.affirm.android.exception.InvalidRequestException
import com.affirm.android.exception.PermissionException
import com.affirm.android.model.AffirmError
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import kotlin.jvm.Throws

internal class AffirmHttpClient {

    fun executeApiRequest(
        request: AffirmHttpRequest
    ): AffirmHttpResponse {
        val response = runCatching {
            execute(request)
        }.getOrElse {
            trackHttpError(request, null)

            throw when (it) {
                is IOException -> APIException("IOException during API request to ${request.url}: ${it.message}", it)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(request, response)
        }

        return response
    }

    @Throws(IOException::class, InvalidRequestException::class)
    private fun execute(request: AffirmHttpRequest): AffirmHttpResponse {
        AffirmLog.i(request.toString())
        AffirmHttpConnection((URL(request.url).openConnection() as HttpsURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            useCaches = false
            requestMethod = request.method.code

            request.headers.forEach { (key, value) ->
                setRequestProperty(key, value)
            }

            if (AffirmHttpRequest.Method.POST == request.method) {
                doOutput = true
                setRequestProperty(HEADER_CONTENT_TYPE, AffirmConstants.CONTENT_TYPE)
                outputStream.use { output -> request.writeBody(output) }
            }
        }).use {
            try {
                val response = it.response
                AffirmLog.i(response.toString())
                return response
            } catch (e: IOException) {
                throw APIException("IOException during API request to ${request.url}: ${e.message}.", e)
            }
        }
    }

    private fun handleApiError(request: AffirmHttpRequest, response: AffirmHttpResponse) {
        trackHttpError(request, response)

        runCatching {
            AffirmPlugins.get().gson().fromJson(response.responseJson.toString(), AffirmError::class.java)
        }.onFailure {
            throw APIException("Some error occurred while parsing the error response", it)
        }.onSuccess {
            val requestId = response.requestId

            when (val responseCode = response.code) {
                HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_NOT_FOUND -> {
                    throw InvalidRequestException(it.message(), it.type(), it.fields(),
                        it.field(), requestId, it.status(), it, null)
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    throw PermissionException(it.message(), requestId, responseCode, it)
                }
                else -> {
                    throw APIException(it.message(), requestId, responseCode, it, null)
                }
            }
        }
    }

    private fun trackHttpError(request: AffirmHttpRequest, response: AffirmHttpResponse?) {
        AffirmTracker.track(
            AffirmTracker.TrackingEvent.NETWORK_ERROR,
            AffirmTracker.TrackingLevel.ERROR,
            AffirmTracker.createTrackingNetworkJsonObj(request, response)
        )
    }

    companion object {
        private const val HEADER_CONTENT_TYPE = "Content-Type"
        private val CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(30).toInt()
        private val READ_TIMEOUT = TimeUnit.SECONDS.toMillis(80).toInt()
    }
}

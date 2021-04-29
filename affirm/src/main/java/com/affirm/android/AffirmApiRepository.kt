package com.affirm.android

import com.affirm.android.exception.APIException
import com.affirm.android.exception.AffirmException
import com.affirm.android.model.CheckoutResponse
import com.affirm.android.model.PromoResponse
import kotlinx.coroutines.*
import org.jetbrains.annotations.NotNull

class AffirmApiRepository : ApiRepository {

    interface AffirmApiListener<T> {
        fun onResponse(@NotNull response: T)
        fun onFailed(exception: AffirmException)
    }

    private val httpClient: AffirmHttpClient by lazy {
        AffirmHttpClient()
    }

    private val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    override fun promoRequest(url: String, listener: AffirmApiListener<PromoResponse>) {
        coroutineScope.launch {
            val result = runCatching {
                requireNotNull(
                    AffirmPlugins.get().gson().fromJson(
                        httpClient.executeApiRequest(AffirmHttpRequest.createGet(url)).body,
                        PromoResponse::class.java
                    )
                )
            }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { listener.onResponse(it) },
                    onFailure = { listener.onFailed(handleError(it)) }
                )
            }
        }
    }

    override fun checkoutRequest(url: String, body: String, listener: AffirmApiListener<CheckoutResponse>) {
        coroutineScope.launch {
            val result = runCatching {
                requireNotNull(
                    AffirmPlugins.get().gson().fromJson(
                        httpClient.executeApiRequest(AffirmHttpRequest.createPost(url, body)).body,
                        CheckoutResponse::class.java
                    )
                )
            }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { listener.onResponse(it) },
                    onFailure = { listener.onFailed(handleError(it)) }
                )
            }
        }
    }

    override fun trackerRequest(url: String, body: String, listener: AffirmApiListener<String>) {
        coroutineScope.launch {
            val result = runCatching {
                requireNotNull(
                    AffirmPlugins.get().gson().fromJson(
                        httpClient.executeApiRequest(AffirmHttpRequest.createPost(url, body)).body,
                        String::class.java
                    )
                )
            }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { listener.onResponse(it) },
                    onFailure = { listener.onFailed(handleError(it)) }
                )
            }
        }
    }

    override fun cancelRequest() {
        coroutineScope.cancel()
    }

    private fun handleError(throwable: Throwable): AffirmException {
        return if (throwable is AffirmException) {
            throwable
        } else {
            APIException(throwable.message, throwable)
        }
    }
}
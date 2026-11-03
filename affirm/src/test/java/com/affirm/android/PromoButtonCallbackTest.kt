package com.affirm.android

import android.view.View
import com.affirm.android.exception.APIException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoButtonCallbackTest {

    private lateinit var button: AffirmPromotionButton
    private lateinit var request: PromoRequest

    @Before
    fun setUp() {
        button = mock(AffirmPromotionButton::class.java)
        doCallRealMethod().`when`(button).isCurrentPromoRequest(any())
        request = mock(PromoRequest::class.java)
        `when`(button.getTag(R.id.affirm_promo_request)).thenReturn(request)
    }

    private fun callback(shownMessage: String?, shownDescription: String?, request: PromoRequest) =
        PromoButtonCallback(button, shownMessage, shownDescription).also { it.request = request }

    @Test
    fun staleSuccessDoesNotUpdateButton() {
        callback(null, null, mock(PromoRequest::class.java)).onPromoWritten("msg", "desc", true)

        verify(button, never()).setLabel(anyString(), anyString())
        verify(button, never()).tag = any()
    }

    @Test
    fun staleFailureDoesNotHideButton() {
        callback(null, null, mock(PromoRequest::class.java)).onFailure(APIException("boom", null))

        verify(button, never()).visibility = anyInt()
    }

    @Test
    fun currentSuccessUpdatesButton() {
        callback(null, null, request).onPromoWritten("msg", "desc", true)

        verify(button).tag = true
        verify(button).setLabel("msg", "desc")
    }

    @Test
    fun sameMessageAsCachedDoesNotReload() {
        callback("msg", "desc", request).onPromoWritten("msg", "desc", false)

        verify(button).tag = false
        verify(button, never()).setLabel(anyString(), anyString())
    }

    @Test
    fun changedMessageReplacesCached() {
        callback("old", "desc", request).onPromoWritten("new", "desc", false)

        verify(button).setLabel("new", "desc")
    }

    @Test
    fun failureWithoutCacheHidesButton() {
        callback(null, null, request).onFailure(APIException("boom", null))

        verify(button).visibility = View.GONE
    }

    @Test
    fun failureWithCacheKeepsButton() {
        callback("msg", "desc", request).onFailure(APIException("boom", null))

        verify(button, never()).visibility = anyInt()
    }
}

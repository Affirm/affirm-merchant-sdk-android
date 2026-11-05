package com.affirm.android

import android.view.View
import com.affirm.android.exception.APIException
import com.affirm.android.model.PromoResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
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
    private lateinit var callback: PromoButtonCallback

    @Before
    fun setUp() {
        button = mock(AffirmPromotionButton::class.java)
        callback = PromoButtonCallback(button)
    }

    @Test
    fun successShowsTheResponse() {
        val response = mock(PromoResponse::class.java)

        callback.onPromoResponse(response, true)

        verify(button).showResponse(response, true)
    }

    @Test
    fun failureHidesEmptyButton() {
        `when`(button.isEmpty).thenReturn(true)

        callback.onFailure(APIException("boom", null))

        verify(button).visibility = View.GONE
    }

    @Test
    fun failureKeepsShownMessage() {
        `when`(button.isEmpty).thenReturn(false)

        callback.onFailure(APIException("boom", null))

        verify(button, never()).visibility = anyInt()
    }
}

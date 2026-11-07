package com.affirm.android

import com.affirm.android.exception.APIException
import com.affirm.android.model.PromoResponse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoButtonCallbackTest {

    private val button = mock(AffirmPromotionButton::class.java)
    private val render = PromoRender(false, false, null, "KEY", "sandbox", { 0 }) { _, _ -> }
    private val callback = PromoButtonCallback(button, render)

    @Test
    fun successGoesToTheButtonWithItsRender() {
        val response = mock(PromoResponse::class.java)

        callback.onSuccess(response, true)

        verify(button).onPromoResponse(render, response, true)
    }

    @Test
    fun failureGoesToTheButtonWithItsRender() {
        callback.onFailure(APIException("boom", null))

        verify(button).onPromoFailure(render)
    }
}

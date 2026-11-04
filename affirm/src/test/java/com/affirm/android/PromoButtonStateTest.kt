package com.affirm.android

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoButtonStateTest {

    private val button = mock(AffirmPromotionButton::class.java, CALLS_REAL_METHODS)

    @Test
    fun destroyCancelsAndClearsCurrentRequest() {
        val request = mock(PromoRequest::class.java)
        button.setTag(R.id.affirm_promo_request, request)

        button.destroy()

        verify(request).cancel()
        assertThat(button.getTag(R.id.affirm_promo_request)).isNull()
    }

    @Test
    fun isShowingTracksCurrentLabel() {
        assertThat(button.hasLabel("msg")).isFalse()

        ReflectionHelpers.setField(button, "message", "msg")

        assertThat(button.hasLabel("msg")).isTrue()
        assertThat(button.hasLabel("other")).isFalse()
    }
}

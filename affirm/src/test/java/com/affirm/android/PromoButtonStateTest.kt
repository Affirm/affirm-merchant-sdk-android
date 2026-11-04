package com.affirm.android

import android.view.View
import com.affirm.android.PromoMessageCache.CachedMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoButtonStateTest {

    private val cached = CachedMessage("ala", "<p>html</p>", true)

    @Before
    fun setUp() {
        // HTML labels read the JS URL from the configuration.
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build()
            )
        }
    }

    @Test
    fun setLabelShowsMessage() {
        val button = plainButton()

        button.setLabel("msg", "desc")

        assertThat(button.hasLabel("msg")).isTrue()
        assertThat(button.hasLabel("other")).isFalse()
        assertThat(button.isEmpty).isFalse()
    }

    @Test
    fun destroyCancelsAndClearsCurrentRequest() {
        val button = plainButton()
        val request = mock(PromoRequest::class.java)
        button.setTag(R.id.affirm_promo_request, request)

        button.destroy()

        verify(request).cancel()
        assertThat(button.getTag(R.id.affirm_promo_request)).isNull()
    }

    @Test
    fun showPlaceholderShowsPlainMessageOnPlainButton() {
        val button = plainButton()

        button.showPlaceholder(cached)

        assertThat(button.hasLabel("ala")).isTrue()
        assertThat(button.tag).isEqualTo(true)
    }

    @Test
    fun showPlaceholderShowsHtmlMessageOnHtmlButton() {
        val button = htmlButton()

        button.showPlaceholder(cached)

        assertThat(button.hasLabel("<p>html</p>")).isTrue()
    }

    @Test
    fun showPlaceholderTreatsEmptyHtmlAsMissOnHtmlButton() {
        val button = htmlButton()
        button.setLabel("<p>previous</p>", "previous")

        button.showPlaceholder(CachedMessage("ala", "", false))

        assertThat(button.isEmpty).isTrue()
    }

    @Test
    fun showPlaceholderMissClearsPreviousConfiguration() {
        val button = plainButton()
        button.setLabel("previous", "previous")

        button.showPlaceholder(null)

        assertThat(button.isEmpty).isTrue()
        assertThat(button.childCount).isEqualTo(0)
    }

    @Test
    fun showPlaceholderMakesHiddenButtonVisible() {
        val button = plainButton()
        button.visibility = View.GONE

        button.showPlaceholder(null)

        assertThat(button.visibility).isEqualTo(View.VISIBLE)
    }

    @Test
    fun showPlaceholderDoesNotReloadShownMessage() {
        val button = spy(plainButton())
        button.setLabel("ala", "ala")

        button.showPlaceholder(cached)

        verify(button, times(1)).setLabel(anyString(), anyString())
        assertThat(button.hasLabel("ala")).isTrue()
    }

    private fun plainButton() = AffirmPromotionButton(RuntimeEnvironment.application)

    private fun htmlButton() = plainButton().apply { configWithHtmlStyling() }
}

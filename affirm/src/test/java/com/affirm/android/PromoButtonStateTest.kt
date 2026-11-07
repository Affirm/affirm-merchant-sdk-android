package com.affirm.android

import android.view.View
import com.affirm.android.PromoMessageCache.CachedMessage
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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

        assertThat(shownText(button)).isEqualTo("msg")
        assertThat(button.isEmpty).isFalse()
    }

    @Test
    fun setLabelSkipsMessageAlreadyShown() {
        val button = spy(plainButton())
        button.setLabel("msg", "desc")

        button.setLabel("msg", "desc")

        verify(button, times(1)).removeAllViews()
    }

    @Test
    fun restyledButtonRedrawsSameMessage() {
        val button = spy(plainButton())
        button.setLabel("msg", "desc")

        button.configWithLocalStyling(
            AffirmColor.AFFIRM_COLOR_TYPE_WHITE,
            AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT
        )
        button.setLabel("msg", "desc")

        verify(button, times(2)).removeAllViews()
    }

    @Test
    fun destroyedHtmlButtonShowsSameMessageOnceStyledAgain() {
        val button = htmlButton()
        button.setLabel("<p>html</p>", "html")
        button.destroy()

        button.configWithHtmlStyling(null, null)
        button.setLabel("<p>html</p>", "html")

        assertThat(shownHtmlDescription(button)).isEqualTo("html")
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
    fun destroyedHtmlButtonIgnoresLabelUntilStyledAgain() {
        val button = htmlButton()
        button.destroy()

        button.setLabel("ala", "ala")
        assertThat(button.isEmpty).isTrue()
        assertThat(button.childCount).isEqualTo(0)

        button.configWithHtmlStyling(null, null)
        button.setLabel("ala", "ala")
        assertThat(shownHtmlDescription(button)).isEqualTo("ala")
    }

    @Test
    fun showPlaceholderShowsPlainMessageOnPlainButton() {
        val button = plainButton()

        button.showPlaceholder(cached)

        assertThat(shownText(button)).isEqualTo("ala")
        assertThat(button.showsPrequal()).isTrue()
    }

    @Test
    fun showPlaceholderShowsHtmlMessageOnHtmlButton() {
        val button = htmlButton()

        button.showPlaceholder(cached)

        // The description is the plain message; the WebView itself shows the HTML one.
        assertThat(shownHtmlDescription(button)).isEqualTo("ala")
        assertThat(button.isEmpty).isFalse()
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

        button.setShowPrequal(true)

        button.showPlaceholder(null)

        assertThat(button.isEmpty).isTrue()
        assertThat(button.childCount).isEqualTo(0)
        assertThat(button.showsPrequal()).isFalse()
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

        verify(button, times(1)).removeAllViews()
    }

    private fun shownText(button: AffirmPromotionButton): String? =
        (button.getChildAt(0) as? PromotionButton)?.text?.toString()

    private fun shownHtmlDescription(button: AffirmPromotionButton): CharSequence? =
        if (button.getChildAt(0) is PromotionWebView) button.contentDescription else null

    private fun plainButton() = AffirmPromotionButton(RuntimeEnvironment.application)

    private fun htmlButton() = plainButton().apply { configWithHtmlStyling() }
}

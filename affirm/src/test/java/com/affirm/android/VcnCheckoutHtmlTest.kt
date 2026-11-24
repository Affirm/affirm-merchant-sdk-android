package com.affirm.android

import android.webkit.WebView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
class VcnCheckoutHtmlTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun checkoutFrameMayAskForTheCameraAndMicrophone() {
        val html = context.resources
            .openRawResource(R.raw.affirm_vcn_checkout)
            .use(AffirmUtils::readInputStream)

        val frame = Regex("<iframe[^>]*>").find(html)?.value

        assertThat(frame).contains("allow=\"camera; microphone\"")
    }

    @Test
    fun checkoutPageReportsTheCheckoutHostAsItsUrl() {
        val webView = WebView(context)

        VcnCheckoutFragment.loadCheckoutHtml(
            webView, "https://uk.affirm.com/products/checkout?token=abc", "<html></html>"
        )

        val load = Shadow.extract<ShadowWebView>(webView).lastLoadDataWithBaseURL
        assertThat(load.baseUrl).isEqualTo("https://uk.affirm.com")
        assertThat(load.historyUrl).isEqualTo("https://uk.affirm.com")
        assertThat(AffirmMediaCaptureHelper.isMediaCapturePage(load.historyUrl)).isTrue()
    }
}

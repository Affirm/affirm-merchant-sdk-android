package com.affirm.android

import android.webkit.WebSettings
import android.widget.FrameLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowWebView

@RunWith(RobolectricTestRunner::class)
class AffirmWebViewTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun webViewsUseTheHttpCache() {
        val webView = AffirmWebView(context)

        assertThat(webView.settings.cacheMode).isEqualTo(WebSettings.LOAD_DEFAULT)
        assertThat(Shadow.extract<ShadowWebView>(webView).wasClearCacheCalled()).isFalse()
    }

    @Test
    fun userAgentKeepsTheSdkVersionMarker() {
        val webView = AffirmWebView(context)

        assertThat(webView.settings.userAgentString)
            .startsWith("Affirm-SDK:Android-" + BuildConfig.VERSION_NAME + " ")
    }

    @Test
    fun mediaPlaybackStillRequiresAUserGestureByDefault() {
        val webView = AffirmWebView(context)

        assertThat(webView.settings.mediaPlaybackRequiresUserGesture).isTrue()
    }

    @Test
    fun destroyWebViewKeepsTheHttpCache() {
        val parent = FrameLayout(context)
        val webView = AffirmWebView(context)
        parent.addView(webView)

        webView.destroyWebView()

        assertThat(parent.childCount).isEqualTo(0)
        assertThat(Shadow.extract<ShadowWebView>(webView).wasClearCacheCalled()).isFalse()
    }
}

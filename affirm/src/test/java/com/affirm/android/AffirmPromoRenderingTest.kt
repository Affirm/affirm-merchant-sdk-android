package com.affirm.android

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AffirmPromoRenderingTest {

    private val template = "<head><script type=\"application/json\" id=\"affirm-promo-payload\">" +
        "{{AFFIRM_PROMO_PAYLOAD}}</script></head>"

    @Test
    fun pageReasonsStayAndUnknownCodesBecomeScriptError() {
        listOf(
            AffirmPromoRenderReason.SANITIZER_UNAVAILABLE,
            AffirmPromoRenderReason.HTML_EMPTY,
            AffirmPromoRenderReason.RESOURCE_FAILED,
            AffirmPromoRenderReason.IMAGE_FAILED,
            AffirmPromoRenderReason.FONT_FAILED,
            AffirmPromoRenderReason.HTML_TIMEOUT,
            AffirmPromoRenderReason.TEMPLATE_ERROR
        ).forEach { assertThat(AffirmPromoRenderReason.fromPage(it)).isEqualTo(it) }
        listOf(null, "", "load_error", "render_process_gone", "<script>").forEach {
            assertThat(AffirmPromoRenderReason.fromPage(it))
                .isEqualTo(AffirmPromoRenderReason.SCRIPT_ERROR)
        }
    }

    @Test
    fun stylesheetUrlsAllowHttpsAndBundleFilesOnly() {
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl(null)).isTrue()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("")).isTrue()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("https://cdn.test/a.css")).isTrue()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("file:///android_asset/a.css"))
            .isTrue()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("file:///android_res/raw/a.css"))
            .isTrue()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("http://cdn.test/a.css")).isFalse()
        assertThat(AffirmPromoRendering.isPermittedStylesheetUrl("file:///sdcard/a.css")).isFalse()
    }

    @Test
    fun cssPixelsMapOntoDevicePixels() {
        assertThat(AffirmPromoRendering.heightForCssPixels(40, 3f)).isEqualTo(120)
        assertThat(AffirmPromoRendering.heightForCssPixels(41, 2.625f)).isEqualTo(108)
        assertThat(AffirmPromoRendering.heightForCssPixels(0, 3f)).isEqualTo(0)
    }

    @Test
    fun documentEscapesPromoHtml() {
        val html = "</script><script>alert(1)</script>"
        val json = AffirmPromoRendering.escapedJsonObject(mapOf("html" to html, "timeoutMs" to 5000))

        assertThat(json).doesNotContain("<")
        assertThat(json).doesNotContain(">")
        assertThat(JSONObject(json).getString("html")).isEqualTo(html)
        assertThat(JSONObject(json).getInt("timeoutMs")).isEqualTo(5000)
    }

    @Test
    fun htmlDocumentPutsSanitizerAndMessageHandlerFirst() {
        val document = AffirmPromoRendering.htmlDocument(template, "var DOMPurify;", mapOf())!!

        assertThat(document).startsWith("<script>var DOMPurify;</script><script>window.webkit=")
        assertThat(document).contains("affirmPromo.postMessage(JSON.stringify(body))")
        assertThat(document).endsWith(template.replace("{{AFFIRM_PROMO_PAYLOAD}}", "{}"))
    }

    @Test
    fun htmlDocumentNeedsExactlyOnePayloadSlot() {
        assertThat(AffirmPromoRendering.htmlDocument("", "", mapOf())).isNull()
        assertThat(AffirmPromoRendering.htmlDocument("<html></html>", "", mapOf())).isNull()
        assertThat(AffirmPromoRendering.htmlDocument(template + template, "", mapOf())).isNull()
    }

    @Test
    fun htmlDocumentWithPayloadUsesTheSharedPage() {
        val resources = RuntimeEnvironment.getApplication().resources
        val document = AffirmPromoRendering.htmlDocumentWithPayload(resources, mapOf("renderId" to "r"))!!

        assertThat(AffirmPromoRendering.sanitizerSource(resources)).isNotEmpty()
        assertThat(AffirmPromoRendering.bundledCss(resources)).isNotEmpty()
        assertThat(document).contains("id=\"affirm-promo-payload\">{\"renderId\":\"r\"}</script>")
        assertThat(document).doesNotContain("{{AFFIRM_PROMO_PAYLOAD}}")
    }

    @Test
    fun fontDeclarationKeepsStyleTextAndFirstStylesheet() {
        val font = AffirmPromoRendering.fontDeclaration(
            "<link rel=\"preconnect\" href=\"https://fonts.test\">" +
                "<link href='https://fonts.test/a.css' rel='stylesheet'>" +
                "<link rel=stylesheet href=https://fonts.test/b.css>" +
                "<STYLE>.a{font-family:A}</STYLE><style type=\"text/css\">.b{}</style>"
        )

        assertThat(font.url).isEqualTo("https://fonts.test/a.css")
        assertThat(font.css).isEqualTo(".a{font-family:A}\n.b{}")
    }

    @Test
    fun fontDeclarationIgnoresCommentedOutMarkup() {
        val font = AffirmPromoRendering.fontDeclaration(
            "<!--Use remote font file-->\n" +
                "<!--<link href=\"https://fonts.test/hanalei.css\" rel=\"stylesheet\">-->\n" +
                "<!--<style>.commented{}</style>-->\n" +
                "<style type=\"text/css\">@font-face{font-family:'L';" +
                "src:url('file:///android_res/font/l.ttf');}</style>"
        )

        assertThat(font.url).isEmpty()
        assertThat(font.css).isEqualTo(
            "@font-face{font-family:'L';src:url('file:///android_res/font/l.ttf');}"
        )
    }

    @Test
    fun fontDeclarationDecodesCharacterReferencesInHref() {
        val font = AffirmPromoRendering.fontDeclaration(
            "<link href=\"https://fonts.test/css?family=A&amp;display=swap&#38;x=&#x31;\" " +
                "rel=\"stylesheet\">"
        )

        assertThat(font.url).isEqualTo("https://fonts.test/css?family=A&display=swap&x=1")
    }

    @Test
    fun emptyFontDeclarationIsEmpty() {
        assertThat(AffirmPromoRendering.fontDeclaration(null).url).isEmpty()
        assertThat(AffirmPromoRendering.fontDeclaration("").css).isEmpty()
    }
}

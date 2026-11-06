package com.affirm.android

import android.content.res.Resources
import android.webkit.URLUtil
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
import com.affirm.android.AffirmConstants.PROMO_SCRIPT_MESSAGE_NAME
import com.affirm.android.AffirmConstants.PROMO_PAYLOAD
import com.google.gson.Gson
import kotlin.math.ceil

@RestrictTo(LIBRARY)
internal object AffirmPromoRenderReason {
    const val LOAD_ERROR = "load_error"
    const val RENDER_PROCESS_GONE = "render_process_gone"
    const val SCRIPT_ERROR = "script_error"
    const val SANITIZER_UNAVAILABLE = "sanitizer_unavailable"
    const val HTML_EMPTY = "html_empty"
    const val RESOURCE_FAILED = "resource_failed"
    const val IMAGE_FAILED = "image_failed"
    const val HTML_TIMEOUT = "html_timeout"
    const val TEMPLATE_ERROR = "template_error"

    private val PAGE_REASONS = setOf(
        SANITIZER_UNAVAILABLE, HTML_EMPTY, RESOURCE_FAILED, IMAGE_FAILED, HTML_TIMEOUT,
        TEMPLATE_ERROR
    )

    /** Page-supplied codes stay as-is. Anything else, including native-only codes, becomes script_error. */
    @JvmStatic
    fun fromPage(reason: String?): String = reason?.takeIf { it in PAGE_REASONS } ?: SCRIPT_ERROR
}

/**
 * HTML promo sandbox: sanitizer, page document and stylesheet rules.
 */
@RestrictTo(LIBRARY)
internal object AffirmPromoRendering {

    const val HTML_TIMEOUT_MS = 5000

    // URLUtil.isResourceUrl is hidden from the SDK.
    private const val RESOURCE_URL_PREFIX = "file:///android_res/"

    // The page posts to window.webkit.messageHandlers; this forwards to the JavascriptInterface
    // of the same name.
    private const val HOST_SHIM = "window.webkit={messageHandlers:{affirmPromo:{postMessage:" +
        "function(body){$PROMO_SCRIPT_MESSAGE_NAME.postMessage(JSON.stringify(body));}}}};"

    private val STYLE = Regex(
        "<style[^>]*>(.*?)</style>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    // An unclosed comment runs to the end.
    private val COMMENT = Regex("<!--.*?(-->|$)", RegexOption.DOT_MATCHES_ALL)
    private val LINK = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    // rel can hold several values, e.g. rel="preload stylesheet".
    private val STYLESHEET = Regex("\\brel\\s*=\\s*[\"']?[^\"'>]*\\bstylesheet\\b", RegexOption.IGNORE_CASE)
    // The value is in double quotes, single quotes or no quotes; one group per form.
    private val HREF = Regex("\\bhref\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE)

    /** https, file:///android_asset/ or file:///android_res/. Empty is allowed. */
    @JvmStatic
    fun isPermittedStylesheetUrl(url: String?): Boolean =
        url.isNullOrEmpty() || URLUtil.isHttpsUrl(url) || URLUtil.isAssetUrl(url) ||
            url.startsWith(RESOURCE_URL_PREFIX)

    /** CSS px from the page. The WebView maps them 1:1 onto dp. */
    @JvmStatic
    fun heightForCssPixels(cssPixels: Int, density: Float): Int =
        if (cssPixels <= 0) 0 else ceil(cssPixels * density.toDouble()).toInt()

    /** JSON object text safe to place in an HTML script element. */
    @JvmStatic
    fun escapedJsonObject(payload: Map<String, Any>): String =
        Gson().toJson(payload).replace("<", "\\u003c").replace(">", "\\u003e")

    @JvmStatic
    fun bundledCss(resources: Resources): String = resource(resources, R.raw.badge_styles)

    @JvmStatic
    fun sanitizerSource(resources: Resources): String = resource(resources, R.raw.dompurify)

    /**
     * Reads affirm_promo.html and inserts the payload as escaped JSON, with the sanitizer and
     * message handler in front of the page. Null if the template is missing.
     */
    @JvmStatic
    fun htmlDocumentWithPayload(resources: Resources, payload: Map<String, Any>): String? =
        htmlDocument(resource(resources, R.raw.affirm_promo), sanitizerSource(resources), payload)

    @VisibleForTesting
    @JvmStatic
    fun htmlDocument(template: String, sanitizer: String, payload: Map<String, Any>): String? {
        val parts = template.split(PROMO_PAYLOAD)
        if (parts.size != 2) return null
        val documentStart = (if (sanitizer.isEmpty()) "" else "<script>$sanitizer</script>") +
            "<script>$HOST_SHIM</script>"
        return documentStart + parts[0] + escapedJsonObject(payload) + parts[1]
    }

    /** A merchant typeface declaration split into the payload's CSS text and font stylesheet. */
    class FontDeclaration(@JvmField val css: String, @JvmField val url: String)

    /**
     * The page takes one font stylesheet URL. `<style>` text is kept as CSS; only the
     * first `<link rel="stylesheet">` is used, and anything else in the declaration is dropped.
     */
    @JvmStatic
    fun fontDeclaration(declaration: String?): FontDeclaration {
        if (declaration.isNullOrEmpty()) return FontDeclaration("", "")
        val markup = COMMENT.replace(declaration, "")
        val css = STYLE.findAll(markup).joinToString("\n") { it.groupValues[1] }
        val urls = LINK.findAll(markup)
            .filter { STYLESHEET.containsMatchIn(it.value) }
            .mapNotNull { link ->
                HREF.find(link.value)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
            }
            // Attribute values may use character references, e.g. `&amp;` in a query string.
            .map { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY).toString() }
            .toList()
        if (urls.size > 1) {
            AffirmLog.w("Promo font declaration has ${urls.size} stylesheets; only the first is used")
        }
        return FontDeclaration(css, urls.firstOrNull().orEmpty())
    }

    private fun resource(resources: Resources, resId: Int): String = try {
        resources.openRawResource(resId).use { it.reader(Charsets.UTF_8).readText() }
    } catch (e: Exception) {
        // Resources.NotFoundException if the resource is missing, IOException if reading fails.
        AffirmLog.e("Failed to load promo resource $resId", e)
        ""
    }
}

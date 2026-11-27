package com.affirm.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal object AffirmMediaCaptureHelper {

    private val PERMISSION_BY_RESOURCE = mapOf(
        PermissionRequest.RESOURCE_VIDEO_CAPTURE to Manifest.permission.CAMERA,
        PermissionRequest.RESOURCE_AUDIO_CAPTURE to Manifest.permission.RECORD_AUDIO
    )

    private val MEDIA_CAPTURE_DOMAINS = listOf("uk.affirm.com")

    @JvmStatic
    fun runtimePermissionsFor(resources: Array<String>?): Array<String> =
        resources.orEmpty().mapNotNull(PERMISSION_BY_RESOURCE::get).toTypedArray()

    @JvmStatic
    fun grantedResources(
        resources: Array<String>?,
        grantedPermissions: Array<String>
    ): Array<String> =
        resources.orEmpty().filter { PERMISSION_BY_RESOURCE[it] in grantedPermissions }
            .toTypedArray()

    /** A warning naming the permissions the app manifest does not declare, or null if none. */
    @JvmStatic
    fun undeclaredPermissionsWarning(context: Context, permissions: Array<String>): String? {
        val undeclared = undeclaredPermissions(context, permissions)
        if (undeclared.isEmpty()) {
            return null
        }
        return "The checkout page requested ${undeclared.joinToString()}, which the app " +
            "manifest does not declare, so Android denied the request without asking the " +
            "user. To support it, add to the app's AndroidManifest.xml:" +
            undeclared.joinToString("") { "\n<uses-permission android:name=\"$it\" />" }
    }

    /** The permissions the app manifest does not declare. Android denies them without a prompt. */
    private fun undeclaredPermissions(context: Context, permissions: Array<String>): List<String> {
        val declared = try {
            context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            return emptyList()
        }
        return permissions.filterNot { it in declared }
    }

    /** Whether the URL is an https page on one of [MEDIA_CAPTURE_DOMAINS] or their subdomains. */
    @JvmStatic
    fun isMediaCapturePage(url: String?): Boolean {
        val uri = parse(url) ?: return false
        if (!uri.scheme.equals(AffirmConstants.HTTPS, ignoreCase = true)) {
            return false
        }
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return MEDIA_CAPTURE_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    /** The lower-case host of the URL, or null when it has none. Logs use it instead of the URL. */
    @JvmStatic
    fun hostOf(url: String?): String? = parse(url)?.host?.lowercase(Locale.US)

    private fun parse(url: String?): URI? = try {
        url?.let(::URI)
    } catch (e: URISyntaxException) {
        null
    }
}

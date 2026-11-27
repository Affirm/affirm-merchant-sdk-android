package com.affirm.android

import android.Manifest
import android.webkit.PermissionRequest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AffirmMediaCaptureHelperTest {

    private val context = RuntimeEnvironment.getApplication()
    private val cameraAndMic = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    private fun declare(vararg permissions: String) {
        shadowOf(context.packageManager)
            .getInternalMutablePackageInfo(context.packageName)
            .requestedPermissions = arrayOf(*permissions)
    }

    @Test
    fun undeclaredPermissionsWarningNamesOnlyWhatTheManifestDoesNotDeclare() {
        declare(Manifest.permission.CAMERA)

        assertThat(AffirmMediaCaptureHelper.undeclaredPermissionsWarning(context, cameraAndMic))
            .isEqualTo(
                "The checkout page requested android.permission.RECORD_AUDIO, which the app " +
                    "manifest does not declare, so Android denied the request without asking " +
                    "the user. To support it, add to the app's AndroidManifest.xml:\n" +
                    "<uses-permission android:name=\"android.permission.RECORD_AUDIO\" />"
            )
    }

    @Test
    fun undeclaredPermissionsWarningListsEveryUndeclaredPermission() {
        declare()

        val warning = AffirmMediaCaptureHelper.undeclaredPermissionsWarning(context, cameraAndMic)

        assertThat(warning).contains(
            "requested android.permission.CAMERA, android.permission.RECORD_AUDIO, which"
        )
        assertThat(warning).endsWith(
            "\n<uses-permission android:name=\"android.permission.CAMERA\" />" +
                "\n<uses-permission android:name=\"android.permission.RECORD_AUDIO\" />"
        )
    }

    @Test
    fun undeclaredPermissionsWarningIsNullWhenTheManifestDeclaresEverything() {
        declare(*cameraAndMic)

        assertThat(AffirmMediaCaptureHelper.undeclaredPermissionsWarning(context, cameraAndMic))
            .isNull()
    }

    @Test
    fun runtimePermissionsForMapsVideoAndAudio() {
        val permissions = AffirmMediaCaptureHelper.runtimePermissionsFor(
            arrayOf(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
            )
        )

        assertThat(permissions).asList()
            .containsExactly(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .inOrder()
    }

    @Test
    fun runtimePermissionsForIgnoresNonMediaResources() {
        val permissions = AffirmMediaCaptureHelper.runtimePermissionsFor(
            arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
        )

        assertThat(permissions).isEmpty()
    }

    @Test
    fun runtimePermissionsForHandlesNull() {
        assertThat(AffirmMediaCaptureHelper.runtimePermissionsFor(null)).isEmpty()
    }

    @Test
    fun grantedResourcesKeepsResourcesWhosePermissionIsGranted() {
        val requested = arrayOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
        )

        assertThat(
            AffirmMediaCaptureHelper.grantedResources(
                requested, arrayOf(Manifest.permission.CAMERA)
            )
        ).asList().containsExactly(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
        assertThat(
            AffirmMediaCaptureHelper.grantedResources(
                requested, arrayOf(Manifest.permission.RECORD_AUDIO)
            )
        ).asList().containsExactly(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        assertThat(AffirmMediaCaptureHelper.grantedResources(requested, emptyArray()))
            .isEmpty()
    }

    @Test
    fun mediaCapturePagesAreHttpsPagesOnUkAffirmCom() {
        listOf(
            "https://uk.affirm.com",
            "https://uk.affirm.com/products/checkout?token=abc",
            "https://sandbox.uk.affirm.com/",
            "https://SANDBOX.UK.Affirm.COM/"
        ).forEach {
            assertThat(AffirmMediaCaptureHelper.isMediaCapturePage(it)).named(it).isTrue()
        }
    }

    @Test
    fun otherPagesAreNotMediaCapturePages() {
        listOf(
            null,
            "",
            "about:blank",
            "http://uk.affirm.com/",
            "https://affirm.com",
            "https://www.affirm.com/",
            "https://api.global-sandbox.affirm.com/",
            "https://www.affirm.ca/",
            "https://evil-uk.affirm.com/",
            "https://uk.affirm.com.evil.com/",
            "https://uk.affirm.com@evil.com/",
            "https://affirm.co.uk/",
            "https://sdk.onfido.com/demo",
            "not a url"
        ).forEach {
            assertThat(AffirmMediaCaptureHelper.isMediaCapturePage(it)).named("$it").isFalse()
        }
    }

    @Test
    fun hostOfKeepsOnlyTheLowerCaseHost() {
        assertThat(AffirmMediaCaptureHelper.hostOf("https://UK.affirm.com/checkout?token=abc"))
            .isEqualTo("uk.affirm.com")
        assertThat(AffirmMediaCaptureHelper.hostOf("about:blank")).isNull()
        assertThat(AffirmMediaCaptureHelper.hostOf(null)).isNull()
    }
}

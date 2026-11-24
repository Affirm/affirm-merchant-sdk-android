package com.affirm.android

import android.Manifest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckoutMediaPermissionsTest {

    private val cameraAndMic = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    private val host = FakeHost()
    private var granted: List<String>? = null

    private val record = AffirmWebChromeClient.MediaPermissionResultCallback { permissions ->
        granted = permissions.toList()
    }

    private fun permissions() = CheckoutMediaPermissions(host)

    @Test
    fun grantedPermissionsAreReportedWithoutAPrompt() {
        host.granted += cameraAndMic

        permissions().request(cameraAndMic, record)

        assertThat(granted).containsExactly(*cameraAndMic).inOrder()
        assertThat(host.requested).isNull()
    }

    @Test
    fun missingPermissionsAreRequestedAndTheResultIsReported() {
        host.granted += Manifest.permission.CAMERA
        val permissions = permissions()

        permissions.request(cameraAndMic, record)

        assertThat(granted).isNull()
        assertThat(host.requested).asList()
            .containsExactly(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .inOrder()
        assertThat(host.requestCode).isEqualTo(CheckoutMediaPermissions.REQUEST_CODE)

        assertThat(permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE))
            .isTrue()
        assertThat(granted).containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun deniedPermissionsAreReported() {
        val permissions = permissions()
        permissions.request(cameraAndMic, record)

        permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE)

        assertThat(granted).isEmpty()
    }

    @Test
    fun otherRequestCodesAreNotHandled() {
        val permissions = permissions()
        permissions.request(cameraAndMic, record)

        assertThat(permissions.onRequestPermissionsResult(1)).isFalse()
        assertThat(granted).isNull()
    }

    @Test
    fun aResultWithoutAPendingRequestIsNotHandled() {
        assertThat(permissions().onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE))
            .isFalse()
    }

    @Test
    fun cancelReportsNothingGrantedAndIgnoresTheLaterResult() {
        host.granted += cameraAndMic.first()
        val permissions = permissions()
        permissions.request(cameraAndMic, record)

        permissions.cancel()

        assertThat(granted).isEmpty()
        granted = null
        assertThat(permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE))
            .isTrue()
        assertThat(granted).isNull()
    }

    @Test
    fun aRequestWhileThePromptIsOpenWaitsForItsResult() {
        val permissions = permissions()
        var first: List<String>? = null
        permissions.request(cameraAndMic) { first = it.toList() }

        permissions.request(cameraAndMic, record)

        assertThat(first).isEmpty()
        assertThat(host.prompts).hasSize(1)
        host.granted += cameraAndMic
        assertThat(permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE))
            .isTrue()
        assertThat(granted).containsExactly(*cameraAndMic).inOrder()
    }

    @Test
    fun aRequestAfterCancelWhileThePromptIsOpenWaitsForItsResult() {
        val permissions = permissions()
        permissions.request(cameraAndMic) { }
        permissions.cancel()

        permissions.request(cameraAndMic, record)

        assertThat(host.prompts).hasSize(1)
        host.granted += Manifest.permission.CAMERA
        permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE)
        assertThat(granted).containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun permissionsTheOpenPromptDidNotAskForAreAskedAfterIt() {
        val permissions = permissions()
        permissions.request(arrayOf(Manifest.permission.CAMERA)) { }
        permissions.request(cameraAndMic, record)
        host.granted += Manifest.permission.CAMERA

        assertThat(permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE))
            .isTrue()

        assertThat(granted).isNull()
        assertThat(host.prompts).containsExactly(
            listOf(Manifest.permission.CAMERA),
            listOf(Manifest.permission.RECORD_AUDIO)
        ).inOrder()
        permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE)
        assertThat(granted).containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun cancelWithoutAPendingRequestReportsNothing() {
        permissions().cancel()

        assertThat(granted).isNull()
    }

    @Test
    fun onlyRequestedPermissionsAreReported() {
        host.granted += cameraAndMic

        permissions().request(arrayOf(Manifest.permission.CAMERA), record)

        assertThat(granted).containsExactly(Manifest.permission.CAMERA)
    }

    @Test
    fun theHostHearsTheCameraIsGrantedBeforeTheResult() {
        var cameraGrantedBeforeResult: Boolean? = null
        val permissions = permissions()
        permissions.request(cameraAndMic) {
            cameraGrantedBeforeResult = host.cameraGrantedCount == 1
        }
        host.granted += Manifest.permission.CAMERA

        permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE)

        assertThat(cameraGrantedBeforeResult).isTrue()
    }

    @Test
    fun theHostHearsTheCameraIsGrantedWithoutAPrompt() {
        host.granted += Manifest.permission.CAMERA

        permissions().request(arrayOf(Manifest.permission.CAMERA), record)

        assertThat(host.cameraGrantedCount).isEqualTo(1)
    }

    @Test
    fun theHostDoesNotHearTheCameraIsGrantedWhenOnlyTheMicrophoneIs() {
        val permissions = permissions()
        permissions.request(cameraAndMic, record)
        host.granted += Manifest.permission.RECORD_AUDIO

        permissions.onRequestPermissionsResult(CheckoutMediaPermissions.REQUEST_CODE)

        assertThat(granted).containsExactly(Manifest.permission.RECORD_AUDIO)
        assertThat(host.cameraGrantedCount).isEqualTo(0)
    }

    @Test
    fun theHostDoesNotHearTheCameraIsGrantedWhenTheRequestIsCancelled() {
        host.granted += Manifest.permission.CAMERA
        val permissions = permissions()
        permissions.request(cameraAndMic, record)

        permissions.cancel()

        assertThat(granted).isEmpty()
        assertThat(host.cameraGrantedCount).isEqualTo(0)
    }

    @Test
    fun requestCodeFitsInTheLower16Bits() {
        assertThat(CheckoutMediaPermissions.REQUEST_CODE and 0xffff.inv()).isEqualTo(0)
    }

    private class FakeHost : CheckoutMediaPermissions.Host {
        val granted = mutableSetOf<String>()
        var requested: Array<String>? = null
        var requestCode: Int? = null
        val prompts = mutableListOf<List<String>>()
        var cameraGrantedCount = 0

        override fun hasPermission(permission: String) = permission in granted

        override fun requestPermissions(permissions: Array<String>, requestCode: Int) {
            requested = permissions
            this.requestCode = requestCode
            prompts += permissions.toList()
        }

        override fun onCameraGranted() {
            cameraGrantedCount++
        }
    }
}

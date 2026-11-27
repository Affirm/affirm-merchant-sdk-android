package com.affirm.android

import android.Manifest
import com.affirm.android.AffirmWebChromeClient.MediaPermissionResultCallback

/**
 * Turns a WebView camera or microphone request into Android runtime permissions for checkout.
 */
internal class CheckoutMediaPermissions(private val host: Host) {

    interface Host {
        fun hasPermission(permission: String): Boolean

        fun requestPermissions(permissions: Array<String>, requestCode: Int)

        /** Called before a result that includes the camera is reported. */
        fun onCameraGranted()
    }

    private class MediaRequest(
        val permissions: Array<String>,
        val callback: MediaPermissionResultCallback
    )

    private var pending: MediaRequest? = null

    // The system answers a second prompt shown while one is open with an empty result at once.
    private var inFlight: Array<String>? = null

    fun request(permissions: Array<String>, callback: MediaPermissionResultCallback) {
        val request = MediaRequest(permissions, callback)
        if (permissions.all(host::hasPermission)) {
            deliverResult(request)
            return
        }
        pending?.callback?.onResult(emptyArray())
        pending = request
        if (inFlight == null) {
            prompt(permissions)
        }
    }

    /** Returns false when the result is not for a media request. */
    fun onRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_CODE) {
            return false
        }
        val asked = inFlight ?: return false
        inFlight = null
        val request = pending ?: return true
        val notAsked = request.permissions.filter { it !in asked && !host.hasPermission(it) }
        if (notAsked.isNotEmpty()) {
            prompt(notAsked.toTypedArray())
            return true
        }
        pending = null
        deliverResult(request)
        return true
    }

    /**
     * Reports nothing granted for the pending request, so the WebView denies it. A result arriving
     * later is not reported.
     */
    fun cancel() {
        val request = pending ?: return
        pending = null
        request.callback.onResult(emptyArray())
    }

    private fun prompt(permissions: Array<String>) {
        inFlight = permissions
        host.requestPermissions(permissions, REQUEST_CODE)
    }

    private fun deliverResult(request: MediaRequest) {
        val granted = request.permissions.filter(host::hasPermission).toTypedArray()
        if (Manifest.permission.CAMERA in granted) {
            host.onCameraGranted()
        }
        request.callback.onResult(granted)
    }

    companion object {
        // Fragment request codes must fit in the lower 16 bits.
        const val REQUEST_CODE = 0xAF01
    }
}

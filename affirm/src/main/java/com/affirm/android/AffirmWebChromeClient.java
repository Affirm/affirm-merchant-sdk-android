package com.affirm.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import static com.affirm.android.AffirmConstants.HTTPS;

class AffirmWebChromeClient extends WebChromeClient {

    interface MediaPermissionResultCallback {
        void onResult(@NonNull String[] grantedPermissions);
    }

    interface Callbacks {
        void chromeLoadCompleted();

        /**
         * The URL of the page the WebView shows. Media requests are only answered on the pages
         * {@link AffirmMediaCaptureHelper#isMediaCapturePage} allows.
         */
        @Nullable
        default String mainFrameUrl() {
            return null;
        }

        default void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull MediaPermissionResultCallback resultCallback) {
            resultCallback.onResult(new String[0]);
        }
    }

    private final Callbacks callback;

    @Nullable
    private PermissionRequest pendingRequest;

    AffirmWebChromeClient(@NonNull Callbacks callback) {
        this.callback = callback;
    }

    @Override
    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                  Message resultMsg) {
        final WebView.HitTestResult result = view.getHitTestResult();
        final String data = result.getExtra();
        if (isUserGesture
                && URLUtil.isNetworkUrl(data)
                && result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            final Context context = view.getContext();
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            context.startActivity(browserIntent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage cm) {
        if (BuildConfig.DEBUG) {
            AffirmLog.d(cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
            return true;
        }
        return false;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        new AlertDialog.Builder(view.getContext()).setTitle(R.string.affirm)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm())
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> result.cancel())
                .create()
                .show();
        return true;
    }

    @Override
    public void onProgressChanged(WebView view, int progress) {
        if (progress > 99) {
            callback.chromeLoadCompleted();
        }
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        final Uri origin = request.getOrigin();
        if (origin == null || !HTTPS.equalsIgnoreCase(origin.getScheme())) {
            deny(request, "origin is not https");
            return;
        }
        final String[] androidPermissions =
                AffirmMediaCaptureHelper.runtimePermissionsFor(request.getResources());
        if (androidPermissions.length == 0) {
            deny(request, "no camera or microphone resource");
            return;
        }
        final String page = callback.mainFrameUrl();
        if (!AffirmMediaCaptureHelper.isMediaCapturePage(page)) {
            deny(request, "page host " + AffirmMediaCaptureHelper.hostOf(page)
                    + " is not allowed to use the camera or microphone");
            return;
        }
        if (pendingRequest != null) {
            deny(request, "another request is pending");
            return;
        }

        pendingRequest = request;
        callback.requestWebViewMediaPermissions(androidPermissions,
                grantedPermissions -> deliverPermissionResult(request, grantedPermissions));
    }

    @Override
    public void onPermissionRequestCanceled(PermissionRequest request) {
        if (pendingRequest == request) {
            pendingRequest = null;
        }
    }

    private void deliverPermissionResult(@NonNull PermissionRequest request,
                                         @NonNull String[] grantedPermissions) {
        if (pendingRequest != request) {
            return;
        }
        pendingRequest = null;
        final String[] granted = AffirmMediaCaptureHelper.grantedResources(
                request.getResources(), grantedPermissions);
        if (granted.length == 0) {
            deny(request, "runtime permission not granted");
            return;
        }
        AffirmLog.d("Granted WebView media request " + Arrays.toString(granted) + " from "
                + request.getOrigin());
        try {
            request.grant(granted);
        } catch (IllegalStateException e) {
            AffirmLog.w("WebView media permission request is no longer valid", e);
        }
    }

    private static void deny(@NonNull PermissionRequest request, @NonNull String reason) {
        AffirmLog.d("Denied WebView media request " + Arrays.toString(request.getResources())
                + " from " + request.getOrigin() + ": " + reason);
        try {
            request.deny();
        } catch (IllegalStateException e) {
            AffirmLog.w("WebView media permission request is no longer valid", e);
        }
    }
}

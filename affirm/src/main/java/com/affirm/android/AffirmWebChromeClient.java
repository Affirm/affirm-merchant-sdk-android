package com.affirm.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;

class AffirmWebChromeClient extends WebChromeClient {

    interface Callbacks {
        void chromeLoadCompleted();
    }

    private final Callbacks callback;

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
}

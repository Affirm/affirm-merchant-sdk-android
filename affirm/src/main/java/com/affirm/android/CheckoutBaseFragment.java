package com.affirm.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.affirm.android.model.Checkout;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;

import org.joda.money.Money;

abstract class CheckoutBaseFragment extends AffirmFragment {

    private CheckoutRequest checkoutRequest;

    // An anonymous host keeps these methods off the public CheckoutFragment API.
    private final CheckoutMediaPermissions mediaPermissions = new CheckoutMediaPermissions(
            new CheckoutMediaPermissions.Host() {
                @Override
                public boolean hasPermission(@NonNull String permission) {
                    final Context context = getContext();
                    return context != null && ContextCompat.checkSelfPermission(context,
                            permission) == PackageManager.PERMISSION_GRANTED;
                }

                @Override
                public void requestPermissions(@NonNull String[] permissions, int requestCode) {
                    CheckoutBaseFragment.this.requestPermissions(permissions, requestCode);
                }

                @Override
                public void onCameraGranted() {
                    // Lets the camera preview of getUserMedia play without a user gesture.
                    if (webView != null) {
                        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
                    }
                }
            });

    private Checkout checkout;

    private Money money;

    private String caas;

    private int cardAuthWindow;

    abstract InnerCheckoutCallback innerCheckoutCallback();

    abstract boolean useVCN();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkout = getArguments().getParcelable(CHECKOUT_EXTRA);
        caas = getArguments().getString(CHECKOUT_CAAS_EXTRA);
        money = (Money) getArguments().getSerializable(CHECKOUT_MONEY);
        cardAuthWindow = getArguments().getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
    }

    @Override
    void onAttached() {
        checkoutRequest = new CheckoutRequest(checkout, innerCheckoutCallback(), caas, money,
                useVCN(), cardAuthWindow);
        checkoutRequest.create();
    }

    @Nullable
    @Override
    public String mainFrameUrl() {
        return webView != null ? webView.getUrl() : null;
    }

    @Override
    public void requestWebViewMediaPermissions(
            @NonNull String[] androidPermissions,
            @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
        final Context context = getContext();
        if (context != null) {
            final String warning = AffirmMediaCaptureHelper.undeclaredPermissionsWarning(
                    context, androidPermissions);
            if (warning != null) {
                AffirmLog.w(warning);
            }
        }
        mediaPermissions.request(androidPermissions, resultCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (!mediaPermissions.onRequestPermissionsResult(requestCode)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onDestroyView() {
        mediaPermissions.cancel();
        // The retained fragment keeps the old WebView alive until onDestroy. Without a chrome
        // client the WebView denies its permission requests instead of asking this fragment.
        if (webView != null) {
            webView.setWebChromeClient(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        checkoutRequest.cancel();
        super.onDestroy();
    }
}

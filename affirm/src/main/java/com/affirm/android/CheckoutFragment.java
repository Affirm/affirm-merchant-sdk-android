package com.affirm.android;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;
import static com.affirm.android.AffirmTracker.createTrackingException;

public final class CheckoutFragment extends CheckoutBaseFragment
        implements CheckoutWebViewClient.Callbacks {

    private static final String CHECKOUT = "Checkout";
    private static final String TAG = TAG_PREFIX + "." + CHECKOUT;

    private Affirm.CheckoutCallbacks listener;

    static CheckoutFragment newInstance(@NonNull AppCompatActivity activity,
                                        @IdRes int containerViewId,
                                        @NonNull Checkout checkout,
                                        @Nullable String caas,
                                        int cardAuthWindow) {
        return newInstance(activity.getSupportFragmentManager(), containerViewId, checkout,
                caas, cardAuthWindow);
    }

    static CheckoutFragment newInstance(@NonNull Fragment fragment,
                                        @IdRes int containerViewId,
                                        @NonNull Checkout checkout,
                                        @Nullable String caas,
                                        int cardAuthWindow) {
        return newInstance(fragment.getChildFragmentManager(), containerViewId, checkout,
                caas, cardAuthWindow);
    }

    private static CheckoutFragment newInstance(@NonNull FragmentManager fragmentManager,
                                                @IdRes int containerViewId,
                                                @NonNull Checkout checkout,
                                                @Nullable String caas,
                                                int cardAuthWindow) {
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return (CheckoutFragment) fragmentManager.findFragmentByTag(TAG);
        }

        CheckoutFragment fragment = new CheckoutFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(CHECKOUT_EXTRA, checkout);
        bundle.putString(CHECKOUT_CAAS_EXTRA, caas);
        bundle.putInt(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);

        fragment.setArguments(bundle);

        addFragment(fragmentManager, containerViewId, fragment, TAG);
        return fragment;
    }

    @Override
    void initViews() {
        webView.setWebViewClient(new CheckoutWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    boolean useVCN() {
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Affirm.CheckoutCallbacks) {
            listener = (Affirm.CheckoutCallbacks) context;
        } else if (getParentFragment() instanceof Affirm.CheckoutCallbacks) {
            listener = (Affirm.CheckoutCallbacks) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    InnerCheckoutCallback innerCheckoutCallback() {
        return new InnerCheckoutCallback() {
            @Override
            public void onError(@NonNull AffirmException exception) {
                AffirmTracker.track(CHECKOUT_CREATION_FAIL, ERROR,
                        createTrackingException(exception));
                removeFragment(TAG);
                if (listener != null) {
                    listener.onAffirmCheckoutError(exception.toString());
                }
            }

            @Override
            public void onSuccess(@NonNull CheckoutResponse response) {
                AffirmTracker.track(CHECKOUT_CREATION_SUCCESS, INFO, null);
                webView.loadUrl(response.redirectUrl());
            }
        };
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(CHECKOUT_WEBVIEW_FAIL, ERROR, createTrackingException(error));
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmCheckoutError(error.toString());
        }
    }

    @Override
    public void onWebViewConfirmation(@NonNull String token) {
        AffirmTracker.track(CHECKOUT_WEBVIEW_SUCCESS, INFO, null);
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmCheckoutSuccess(token);
        }
    }

    @Override
    public void onWebViewCancellation() {
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmCheckoutCancelled();
        }
    }
}
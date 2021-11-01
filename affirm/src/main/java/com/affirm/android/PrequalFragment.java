package com.affirm.android;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.affirm.android.exception.ConnectionException;

import java.math.BigDecimal;

import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_IS_SDK;
import static com.affirm.android.AffirmConstants.PREQUAL_PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PREQUAL_PATH;
import static com.affirm.android.AffirmConstants.PREQUAL_PROMO_EXTERNAL_ID;
import static com.affirm.android.AffirmConstants.PREQUAL_PUBLIC_API_KEY;
import static com.affirm.android.AffirmConstants.PREQUAL_REFERRING_URL;
import static com.affirm.android.AffirmConstants.PREQUAL_UNIT_PRICE;
import static com.affirm.android.AffirmConstants.PREQUAL_USE_PROMO;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.REFERRING_URL;
import static com.affirm.android.AffirmTracker.TrackingEvent.PREQUAL_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingExceptionJsonObj;

public final class PrequalFragment extends AffirmFragment
        implements PrequalWebViewClient.Callbacks {

    private static final String PREQUAL = "Prequal";
    private static final String TAG = TAG_PREFIX + "." + PREQUAL;

    private Affirm.PrequalCallbacks listener;

    private String amount;
    private String promoId;
    private String pageType;

    static PrequalFragment newInstance(@NonNull AppCompatActivity activity,
                                       @IdRes int containerViewId,
                                       @NonNull BigDecimal amount,
                                       @Nullable String promoId,
                                       @Nullable String pageType) {
        return newInstance(activity.getSupportFragmentManager(), containerViewId, amount,
                promoId, pageType);
    }

    static PrequalFragment newInstance(@NonNull Fragment fragment,
                                       @IdRes int containerViewId,
                                       @NonNull BigDecimal amount,
                                       @Nullable String promoId,
                                       @Nullable String pageType) {
        return newInstance(fragment.getChildFragmentManager(), containerViewId, amount,
                promoId, pageType);
    }

    private static PrequalFragment newInstance(@NonNull FragmentManager fragmentManager,
                                               @IdRes int containerViewId,
                                               @NonNull BigDecimal amount,
                                               @Nullable String promoId,
                                               @Nullable String pageType) {
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return (PrequalFragment) fragmentManager.findFragmentByTag(TAG);
        }

        String stringAmount = String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        PrequalFragment fragment = new PrequalFragment();
        Bundle bundle = new Bundle();
        bundle.putString(AMOUNT, stringAmount);
        bundle.putString(PROMO_ID, promoId);
        bundle.putString(PAGE_TYPE, pageType);
        fragment.setArguments(bundle);

        addFragment(fragmentManager, containerViewId, fragment, TAG);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.requireNonNull(getArguments(), "mArguments cannot be null");

        amount = getArguments().getString(AMOUNT);
        promoId = getArguments().getString(PROMO_ID);
        pageType = getArguments().getString(PAGE_TYPE);
    }

    @Override
    void initViews() {
        webView.setWebViewClient(new PrequalWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    void onAttached() {
        String publicKey = AffirmPlugins.get().publicKey();
        String prequalUri = HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl() + PREQUAL_PATH;
        Uri.Builder builder = Uri.parse(prequalUri).buildUpon();
        builder.appendQueryParameter(PREQUAL_PUBLIC_API_KEY, publicKey);
        builder.appendQueryParameter(PREQUAL_UNIT_PRICE, amount);
        builder.appendQueryParameter(PREQUAL_USE_PROMO, "true");
        builder.appendQueryParameter(PREQUAL_IS_SDK, "true");
        builder.appendQueryParameter(PREQUAL_REFERRING_URL, REFERRING_URL);
        if (promoId != null) {
            builder.appendQueryParameter(PREQUAL_PROMO_EXTERNAL_ID, promoId);
        }
        if (pageType != null) {
            builder.appendQueryParameter(PREQUAL_PAGE_TYPE, pageType);
        }
        webView.loadUrl(builder.build().toString());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Affirm.PrequalCallbacks) {
            listener = (Affirm.PrequalCallbacks) context;
        } else if (getParentFragment() instanceof Affirm.PrequalCallbacks) {
            listener = (Affirm.PrequalCallbacks) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(PREQUAL_WEBVIEW_FAIL, ERROR, createTrackingExceptionJsonObj(error));
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmPrequalError(error.toString());
        }
    }

    @Override
    public void onWebViewConfirmation() {
        removeFragment(TAG);
        if (getActivity() != null && getActivity() instanceof PrequalActivity) {
            getActivity().finish();
        }
    }
}
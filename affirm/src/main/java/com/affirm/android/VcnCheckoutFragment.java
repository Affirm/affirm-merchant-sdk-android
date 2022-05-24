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

import com.affirm.android.exception.AffirmException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;
import com.affirm.android.model.VcnReason;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CONFIRMATION_URL;
import static com.affirm.android.AffirmConstants.CANCELLED_CB_URL;
import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_MONEY;
import static com.affirm.android.AffirmConstants.CHECKOUT_RECEIVE_REASON_CODES;
import static com.affirm.android.AffirmConstants.CONFIRM_CB_URL;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.NEW_FLOW;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.URL;
import static com.affirm.android.AffirmConstants.URL2;
import static com.affirm.android.AffirmConstants.UTF_8;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_WEBVIEW_SUCCESS;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;
import static com.affirm.android.AffirmTracker.createTrackingException;

import org.joda.money.Money;

public final class VcnCheckoutFragment extends CheckoutBaseFragment
        implements VcnCheckoutWebViewClient.Callbacks {

    private static final String VCN_CHECKOUT = "VCN_Checkout";
    private static final String TAG = TAG_PREFIX + "." + VCN_CHECKOUT;

    private String receiveReasonCodes;

    private Affirm.VcnCheckoutCallbacks listener;

    static VcnCheckoutFragment newInstance(@NonNull AppCompatActivity activity,
                                           @IdRes int containerViewId,
                                           @NonNull Checkout checkout,
                                           String receiveReasonCodes,
                                           @Nullable String caas,
                                           @Nullable Money money,
                                           int cardAuthWindow,
                                           boolean newFlow) {
        return newInstance(activity.getSupportFragmentManager(), containerViewId, checkout,
                receiveReasonCodes, caas, money, cardAuthWindow, newFlow);
    }

    static VcnCheckoutFragment newInstance(@NonNull Fragment fragment,
                                           @IdRes int containerViewId,
                                           @NonNull Checkout checkout,
                                           String receiveReasonCodes,
                                           @Nullable String caas,
                                           @Nullable Money money,
                                           int cardAuthWindow,
                                           boolean newFlow) {
        return newInstance(fragment.getChildFragmentManager(), containerViewId, checkout,
                receiveReasonCodes, caas, money, cardAuthWindow, newFlow);
    }

    private static VcnCheckoutFragment newInstance(@NonNull FragmentManager fragmentManager,
                                                   @IdRes int containerViewId,
                                                   @NonNull Checkout checkout,
                                                   String receiveReasonCodes,
                                                   @Nullable String caas,
                                                   @Nullable Money money,
                                                   int cardAuthWindow,
                                                   boolean newFlow) {
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return (VcnCheckoutFragment) fragmentManager.findFragmentByTag(TAG);
        }

        VcnCheckoutFragment fragment = new VcnCheckoutFragment();
        Bundle bundle = new Bundle();
        bundle.putString(CHECKOUT_RECEIVE_REASON_CODES, receiveReasonCodes);
        bundle.putParcelable(CHECKOUT_EXTRA, checkout);
        bundle.putString(CHECKOUT_CAAS_EXTRA, caas);
        bundle.putSerializable(CHECKOUT_MONEY, money);
        bundle.putInt(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
        bundle.putBoolean(NEW_FLOW, newFlow);
        fragment.setArguments(bundle);

        addFragment(fragmentManager, containerViewId, fragment, TAG);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiveReasonCodes = getArguments().getString(CHECKOUT_RECEIVE_REASON_CODES);
    }

    @Override
    void initViews() {
        webView.setWebViewClient(new VcnCheckoutWebViewClient(
                AffirmPlugins.get().gson(), receiveReasonCodes, this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    boolean useVCN() {
        return true;
    }

    @Override
    InnerCheckoutCallback innerCheckoutCallback() {
        return new InnerCheckoutCallback() {
            @Override
            public void onError(@NonNull AffirmException exception) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_FAIL, ERROR,
                        createTrackingException(exception));
                removeFragment(TAG);
                if (listener != null) {
                    listener.onAffirmVcnCheckoutError(exception.toString());
                }
            }

            @Override
            public void onSuccess(@NonNull CheckoutResponse response) {
                AffirmTracker.track(VCN_CHECKOUT_CREATION_SUCCESS, INFO, null);
                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());
                webView.loadDataWithBaseURL(HTTPS_PROTOCOL + uri.getHost(), html,
                        TEXT_HTML, UTF_8, null);
            }
        };
    }

    private String initialHtml(@NonNull CheckoutResponse response) {
        String html;
        InputStream ins = null;
        try {
            ins = getResources().openRawResource(R.raw.affirm_vcn_checkout);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            AffirmUtils.closeInputStream(ins);
        }

        final HashMap<String, String> map = new HashMap<>();

        map.put(URL, response.redirectUrl());
        map.put(URL2, response.redirectUrl());
        map.put(CONFIRM_CB_URL, AFFIRM_CHECKOUT_CONFIRMATION_URL);
        map.put(CANCELLED_CB_URL, AFFIRM_CHECKOUT_CANCELLATION_URL);
        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Affirm.VcnCheckoutCallbacks) {
            listener = (Affirm.VcnCheckoutCallbacks) context;
        } else if (getParentFragment() instanceof Affirm.VcnCheckoutCallbacks) {
            listener = (Affirm.VcnCheckoutCallbacks) getParentFragment();
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    public void onWebViewConfirmation(@NonNull CardDetails cardDetails) {
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_SUCCESS, INFO, null);
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmVcnCheckoutSuccess(cardDetails);
        }
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(VCN_CHECKOUT_WEBVIEW_FAIL, ERROR,
                createTrackingException(error));
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmVcnCheckoutError(error.toString());
        }
    }

    @Override
    public void onWebViewCancellation() {
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmVcnCheckoutCancelled();
        }
    }

    @Override
    public void onWebViewCancellationReason(@NonNull VcnReason vcnReason) {
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmVcnCheckoutCancelledReason(vcnReason);
        }
    }
}
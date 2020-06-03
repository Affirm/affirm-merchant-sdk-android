package com.affirm.android;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.affirm.android.exception.ConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.AFFIRM_CHECKOUT_CANCELLATION_URL;
import static com.affirm.android.AffirmConstants.AMOUNT;
import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.CANCEL_URL;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.MAP_EXTRA;
import static com.affirm.android.AffirmConstants.MODAL_ID;
import static com.affirm.android.AffirmConstants.PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_ID;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.TYPE_EXTRA;
import static com.affirm.android.AffirmConstants.UTF_8;
import static com.affirm.android.AffirmTracker.TrackingEvent.PRODUCT_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingEvent.SITE_WEBVIEW_FAIL;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;

public final class ModalFragment extends AffirmFragment implements ModalWebViewClient.Callbacks {

    enum ModalType {
        PRODUCT(R.raw.affirm_modal_template, PRODUCT_WEBVIEW_FAIL),
        SITE(R.raw.affirm_modal_template, SITE_WEBVIEW_FAIL);

        @RawRes
        final int templateRes;
        final AffirmTracker.TrackingEvent failureEvent;

        ModalType(int templateRes, AffirmTracker.TrackingEvent failureEvent) {
            this.templateRes = templateRes;
            this.failureEvent = failureEvent;
        }
    }

    private static final String MODAL = "Modal";
    private static final String TAG = TAG_PREFIX + "." + MODAL;

    private ModalType type;

    private HashMap<String, String> map;

    private Affirm.PrequalCallbacks listener;

    private ModalFragment() {
    }

    static AffirmFragment newInstance(@NonNull AppCompatActivity activity,
                                      @IdRes int containerViewId,
                                      @NonNull BigDecimal amount, @NonNull ModalType type,
                                      @Nullable String modalId, @Nullable String pageType,
                                      @Nullable String promoId) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return (ModalFragment) fragmentManager.findFragmentByTag(TAG);
        }

        final String stringAmount =
                String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(amount));
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseJsUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();
        map.put(AMOUNT, stringAmount);
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(CANCEL_URL, AFFIRM_CHECKOUT_CANCELLATION_URL);
        map.put(MODAL_ID, modalId == null ? "" : modalId);
        map.put(PAGE_TYPE, pageType == null ? "" : pageType);
        map.put(PROMO_ID, promoId == null ? "" : promoId);

        ModalFragment fragment = new ModalFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(TYPE_EXTRA, type);
        bundle.putSerializable(MAP_EXTRA, map);
        fragment.setArguments(bundle);

        addFragment(fragmentManager, containerViewId, fragment, TAG);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AffirmUtils.requireNonNull(getArguments(), "mArguments cannot be null");

        map = (HashMap<String, String>) getArguments().getSerializable(MAP_EXTRA);
        type = (ModalType) getArguments().getSerializable(TYPE_EXTRA);
    }

    @Override
    void initViews() {
        webView.setWebViewClient(new ModalWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Affirm.PrequalCallbacks) {
            listener = (Affirm.PrequalCallbacks) context;
        }
    }

    @Override
    public void onDetach() {
        listener = null;
        super.onDetach();
    }

    @Override
    void onAttached() {
        final String html = initialHtml();
        webView.loadDataWithBaseURL(
                HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl(),
                html, TEXT_HTML, UTF_8, null);
    }

    private String initialHtml() {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(type.templateRes);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onWebViewCancellation() {
        removeFragment(TAG);
        if (getActivity() != null && getActivity() instanceof ModalActivity) {
            getActivity().finish();
        }
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmTracker.track(type.failureEvent, ERROR, null);
        removeFragment(TAG);
        if (listener != null) {
            listener.onAffirmPrequalError(error.toString());
        }
    }
}

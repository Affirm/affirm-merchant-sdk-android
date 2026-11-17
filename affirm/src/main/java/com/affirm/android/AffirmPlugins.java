package com.affirm.android;

import android.os.Bundle;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.CardDetailsInner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AffirmPlugins {

    private static final Object LOCK = new Object();
    private static AffirmPlugins instance;

    private static final String SAVED_STATE_KEY = "com.affirm.android.AffirmPlugins.state";
    private static final String STATE_PUBLIC_KEY = "public_key";
    private static final String STATE_ENVIRONMENT = "environment";
    private static final String STATE_MERCHANT_NAME = "merchant_name";
    private static final String STATE_CARD_TIP = "card_tip";
    private static final String STATE_LOCALE = "locale";
    private static final String STATE_COUNTRY_CODE = "country_code";
    private static final String STATE_USE_LEGACY_NATIVE_RENDERING = "use_legacy_native_rendering";
    private static final String STATE_CHECKOUT_REQUEST = "checkout_request";
    private static final String STATE_VCN_CHECKOUT_REQUEST = "vcn_checkout_request";
    private static final String STATE_PREQUAL_REQUEST = "prequal_request";
    private static final String STATE_RECEIVE_REASON_CODES = "receive_reason_codes";
    private static final String STATE_LOG_LEVEL = "log_level";

    private Affirm.Configuration configuration;
    private boolean restored;

    private AffirmHttpClient restClient;
    private Gson gson;

    private CardDetailsInner cardDetailsInner;

    AffirmPlugins(@NonNull Affirm.Configuration configuration) {
        this.configuration = configuration;
    }

    Affirm.Configuration getConfiguration() {
        return configuration;
    }

    void setConfiguration(Affirm.Configuration configuration) {
        this.configuration = configuration;
        PromoMessageCache.evictAll();
    }

    /**
     * Ignored when the host app has already initialized Affirm, but replaces a configuration
     * restored by {@link #restoreIfNeeded(Bundle)}.
     */
    static void initialize(@NonNull Affirm.Configuration configuration) {
        synchronized (LOCK) {
            if (instance != null && !instance.restored) {
                AffirmLog.w("Affirm is already initialized");
                return;
            }
            instance = new AffirmPlugins(configuration);
        }
        PromoMessageCache.evictAll();
    }

    static void saveState(@NonNull Bundle outState) {
        AffirmPlugins plugins = get();
        if (plugins == null) {
            return;
        }
        Affirm.Configuration configuration = plugins.getConfiguration();
        Bundle state = new Bundle();
        state.putString(STATE_PUBLIC_KEY, configuration.publicKey);
        state.putString(STATE_ENVIRONMENT, configuration.environment.name());
        state.putString(STATE_MERCHANT_NAME, configuration.merchantName);
        state.putString(STATE_CARD_TIP, configuration.cardTip);
        state.putString(STATE_LOCALE, configuration.locale);
        state.putString(STATE_COUNTRY_CODE, configuration.countryCode);
        state.putBoolean(STATE_USE_LEGACY_NATIVE_RENDERING,
                configuration.useLegacyNativeRendering);
        state.putInt(STATE_CHECKOUT_REQUEST, Affirm.getCheckoutRequest());
        state.putInt(STATE_VCN_CHECKOUT_REQUEST, Affirm.getVcnCheckoutRequest());
        state.putInt(STATE_PREQUAL_REQUEST, Affirm.getPrequalRequest());
        state.putString(STATE_RECEIVE_REASON_CODES, Affirm.getReceiveReasonCodes());
        state.putInt(STATE_LOG_LEVEL, AffirmLog.getLogLevel());
        outState.putBundle(SAVED_STATE_KEY, state);
    }

    /**
     * Re-initializes from {@link #saveState(Bundle)} when an Affirm screen is restored in a new
     * process before the host app has called {@link Affirm#initialize(Affirm.Configuration)}.
     * A later {@link Affirm#initialize(Affirm.Configuration)} replaces the restored configuration.
     */
    static void restoreIfNeeded(@Nullable Bundle savedInstanceState) {
        Bundle state = savedInstanceState != null
                ? savedInstanceState.getBundle(SAVED_STATE_KEY)
                : null;
        if (state == null) {
            return;
        }
        Affirm.Configuration configuration = readConfiguration(state);
        if (configuration == null) {
            AffirmLog.e("Failed to restore Affirm configuration");
            return;
        }
        synchronized (LOCK) {
            if (instance != null) {
                return;
            }
            AffirmLog.setLogLevel(state.getInt(STATE_LOG_LEVEL, AffirmLog.getLogLevel()));
            Affirm.setCheckoutRequest(state.getInt(STATE_CHECKOUT_REQUEST));
            Affirm.setVcnCheckoutRequest(state.getInt(STATE_VCN_CHECKOUT_REQUEST));
            Affirm.setPrequalRequest(state.getInt(STATE_PREQUAL_REQUEST));
            Affirm.setReceiveReasonCodes(state.getString(STATE_RECEIVE_REASON_CODES));
            AffirmPlugins plugins = new AffirmPlugins(configuration);
            plugins.restored = true;
            instance = plugins;
        }
        AffirmLog.w("Affirm was not initialized, so the configuration of the previous process "
                + "was restored. Call Affirm.initialize() in Application.onCreate()");
    }

    @Nullable
    private static Affirm.Configuration readConfiguration(@NonNull Bundle state) {
        String publicKey = state.getString(STATE_PUBLIC_KEY);
        String environmentName = state.getString(STATE_ENVIRONMENT);
        if (publicKey == null || environmentName == null) {
            return null;
        }
        Affirm.Environment environment;
        try {
            environment = Affirm.Environment.valueOf(environmentName);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new Affirm.Configuration.Builder(publicKey, environment)
                .setMerchantName(state.getString(STATE_MERCHANT_NAME))
                .setCardTip(state.getString(STATE_CARD_TIP))
                .setLocale(state.getString(STATE_LOCALE))
                .setCountryCode(state.getString(STATE_COUNTRY_CODE))
                .setUseLegacyNativeRendering(
                        state.getBoolean(STATE_USE_LEGACY_NATIVE_RENDERING))
                .build();
    }

    static boolean isRestored() {
        synchronized (LOCK) {
            return instance != null && instance.restored;
        }
    }

    public static AffirmPlugins get() {
        synchronized (LOCK) {
            return instance;
        }
    }

    static void reset() {
        synchronized (LOCK) {
            instance = null;
        }
        PromoMessageCache.evictAll();
    }

    public CardDetailsInner getCachedCardDetails() {
        return cardDetailsInner;
    }

    public void setCacheCardDetails(CardDetailsInner cardDetailsInner) {
        this.cardDetailsInner = cardDetailsInner;
    }

    public String publicKey() {
        return configuration.publicKey;
    }

    String merchantName() {
        return configuration.merchantName;
    }

    String cardTip() {
        return configuration.cardTip;
    }

    Affirm.Environment environment() {
        return configuration.environment;
    }

    String locale() {
        return configuration.locale;
    }

    String countryCode() {
        return configuration.countryCode;
    }

    boolean useLegacyNativeRendering() {
        return configuration.useLegacyNativeRendering;
    }

    String environmentName() {
        return configuration.environment.name();
    }

    String checkoutUrl() {
        return configuration.environment.checkoutUrl();
    }

    String promoUrl() {
        return configuration.environment.promoUrl(countryCode());
    }

    String jsUrl() {
        return configuration.environment.jsUrl();
    }

    String trackerUrl() {
        return configuration.environment.trackerUrl(countryCode());
    }

    String invalidCheckoutRedirectUrl() {
        return configuration.environment.invalidCheckoutRedirectUrl();
    }

    synchronized Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(AffirmAdapterFactory.create())
                    .create();
        }
        return gson;
    }

    synchronized AffirmHttpClient restClient() {
        if (restClient == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            //add it as the first interceptor
            clientBuilder.interceptors().add(0, chain -> {
                final Request.Builder builder = chain.request().newBuilder();
                builder.addHeader("Accept", "application/json");
                builder.addHeader("Content-Type", "application/json");
                builder.addHeader("Affirm-User-Agent", "Affirm-Android-SDK");
                builder.addHeader("Affirm-User-Agent-Version", BuildConfig.VERSION_NAME);
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(
                        AffirmConstants.HTTPS_PROTOCOL + promoUrl()
                );
                if (cookie != null) {
                    builder.addHeader("Cookie", cookie);
                }
                return chain.proceed(builder.build());
            });
            clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
            clientBuilder.readTimeout(30, TimeUnit.SECONDS);
            clientBuilder.followRedirects(false);
            restClient = AffirmHttpClient.createClient(clientBuilder);
        }
        return restClient;
    }
}

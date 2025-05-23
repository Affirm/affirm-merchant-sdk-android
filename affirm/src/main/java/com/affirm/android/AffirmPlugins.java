package com.affirm.android;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

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
    private Affirm.Configuration configuration;

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
    }

    static void initialize(@NonNull Affirm.Configuration configuration) {
        AffirmPlugins.set(new AffirmPlugins(configuration));
    }

    private static void set(@NonNull AffirmPlugins plugins) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("AffirmPlugins is already initialized");
            }
            instance = plugins;
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

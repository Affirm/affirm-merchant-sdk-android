package com.affirm.android;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.affirm.android.model.AbstractAddress;
import com.affirm.android.model.AddressSerializer;
import com.affirm.android.model.AffirmAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;

class AffirmPlugins {

    private static final Object LOCK = new Object();
    private static AffirmPlugins instance;
    private final Affirm.Configuration configuration;

    private AffirmHttpClient restClient;
    private Gson gson;

    AffirmPlugins(@NonNull Affirm.Configuration configuration) {
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

    String publicKey() {
        return configuration.publicKey;
    }

    String merchantName() {
        return configuration.merchantName;
    }

    Affirm.Environment environment() {
        return configuration.environment;
    }

    String environmentName() {
        return configuration.environment.name();
    }

    String baseUrl() {
        return configuration.environment.baseUrl();
    }

    String basePromoUrl() {
        return configuration.environment.basePromoUrl();
    }

    String baseJsUrl() {
        return configuration.environment.baseJsUrl();
    }

    String trackerBaseUrl() {
        return configuration.environment.trackerBaseUrl();
    }

    String baseInvalidCheckoutRedirectUrl() {
        return configuration.environment.baseInvalidCheckoutRedirectUrl();
    }

    String privateKey() {
        return configuration.privateKey;
    }

    synchronized Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(AffirmAdapterFactory.create())
                    .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
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

                if (privateKey() != null) {
                    final String basic = Credentials.basic(publicKey(), privateKey());
                    builder.addHeader("Authorization", basic);
                }

                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager
                        .getCookie(AffirmConstants.HTTPS_PROTOCOL + baseUrl());
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

package com.affirm.android;

import com.affirm.android.model.AffirmAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        return configuration.environment.baseUrl;
    }

    String trackerBaseUrl() {
        return configuration.environment.trackerBaseUrl;
    }

    synchronized Gson gson() {
        if (gson == null) {
            gson =
                    new GsonBuilder()
                            .registerTypeAdapterFactory(AffirmAdapterFactory.create()).create();
        }
        return gson;
    }

    synchronized AffirmHttpClient restClient() {
        if (restClient == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            //add it as the first interceptor
            clientBuilder.interceptors().add(0, new Interceptor() {
                @Override
                @NonNull
                public Response intercept(@NonNull Chain chain) throws IOException {
                    final Request request = chain.request()
                            .newBuilder()
                            .addHeader("Accept", "application/json")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Affirm-User-Agent", "Affirm-Android-SDK")
                            .addHeader("Affirm-User-Agent-Version", BuildConfig.VERSION_NAME)
                            .build();

                    return chain.proceed(request);
                }
            });
            clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
            clientBuilder.readTimeout(30, TimeUnit.SECONDS);
            clientBuilder.followRedirects(false);
            restClient = AffirmHttpClient.createClient(clientBuilder);
        }
        return restClient;
    }
}

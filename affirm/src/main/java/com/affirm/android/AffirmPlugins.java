package com.affirm.android;

import com.affirm.android.model.MyAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

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

    private final Object lock = new Object();

    private AffirmPlugins(Affirm.Configuration configuration) {
        this.configuration = configuration;
    }

    static void initialize(Affirm.Configuration configuration) {
        AffirmPlugins.set(new AffirmPlugins(configuration));
    }

    static void set(AffirmPlugins plugins) {
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

    public String publicKey() {
        return configuration.publicKey;
    }

    public String name() {
        return configuration.name;
    }

    public Affirm.Environment environment() {
        return configuration.environment;
    }

    public String baseUrl() {
        return configuration.environment.baseUrl;
    }

    public String trackerBaseUrl() {
        return configuration.environment.trackerBaseUrl;
    }

    Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder().registerTypeAdapterFactory(MyAdapterFactory.create()).create();
        }
        return gson;
    }

    AffirmHttpClient restClient() {
        synchronized (lock) {
            if (restClient == null) {
                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                //add it as the first interceptor
                clientBuilder.interceptors().add(0, new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
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
                restClient = AffirmHttpClient.createClient(clientBuilder);
            }
            return restClient;
        }
    }
}

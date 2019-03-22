package com.affirm.android;

import com.affirm.android.model.MyAdapterFactory;
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
    private static AffirmPlugins mInstance;
    private final Affirm.Configuration mConfiguration;

    private AffirmHttpClient mRestClient;
    private Gson mGson;

    AffirmPlugins(Affirm.Configuration configuration) {
        mConfiguration = configuration;
    }

    static void initialize(Affirm.Configuration configuration) {
        AffirmPlugins.set(new AffirmPlugins(configuration));
    }

    private static void set(AffirmPlugins plugins) {
        synchronized (LOCK) {
            if (mInstance != null) {
                throw new IllegalStateException("AffirmPlugins is already initialized");
            }
            mInstance = plugins;
        }
    }

    public static AffirmPlugins get() {
        synchronized (LOCK) {
            return mInstance;
        }
    }

    static void reset() {
        synchronized (LOCK) {
            mInstance = null;
        }
    }

    String publicKey() {
        return mConfiguration.publicKey;
    }

    String merchantName() {
        return mConfiguration.merchantName;
    }

    Affirm.Environment environment() {
        return mConfiguration.environment;
    }

    String environmentName() {
        return mConfiguration.environment.name();
    }

    String baseUrl() {
        return mConfiguration.environment.baseUrl;
    }

    String trackerBaseUrl() {
        return mConfiguration.environment.trackerBaseUrl;
    }

    synchronized Gson gson() {
        if (mGson == null) {
            mGson =
                new GsonBuilder().registerTypeAdapterFactory(MyAdapterFactory.create()).create();
        }
        return mGson;
    }

    synchronized AffirmHttpClient restClient() {
        if (mRestClient == null) {
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
            mRestClient = AffirmHttpClient.createClient(clientBuilder);
        }
        return mRestClient;
    }
}

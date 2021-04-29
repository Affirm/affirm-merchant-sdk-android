package com.affirm.android;

import com.affirm.android.model.AbstractAddress;
import com.affirm.android.model.AffirmAdapterFactory;
import com.affirm.android.model.AddressSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import androidx.annotation.NonNull;

class AffirmPlugins {

    private static final Object LOCK = new Object();
    private static AffirmPlugins instance;
    private final Affirm.Configuration configuration;
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

    synchronized Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(AffirmAdapterFactory.create())
                    .registerTypeAdapter(AbstractAddress.class, new AddressSerializer())
                    .create();
        }
        return gson;
    }
}

package com.affirm.android;

import android.app.Activity;
import android.util.Log;

import com.affirm.android.model.Checkout;

import androidx.annotation.NonNull;

public class Affirm {

    public static final int LOG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int LOG_LEVEL_DEBUG = Log.DEBUG;
    public static final int LOG_LEVEL_INFO = Log.INFO;
    public static final int LOG_LEVEL_WARNING = Log.WARN;
    public static final int LOG_LEVEL_ERROR = Log.ERROR;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;

    private static final int CHECKOUT_REQUEST = 8076;
    private static final int VCN_CHECKOUT_REQUEST = 8077;

    /**
     * Returns the level of logging that will be displayed.
     */
    public static int getLogLevel() {
        return AffirmLog.getLogLevel();
    }

    public static void initialize(Configuration configuration) {


        AffirmPlugins.initialize(configuration);
    }


    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout) {
        CheckoutActivity.startActivity(activity, CHECKOUT_REQUEST, checkout);

    }

    public static void startVcnCheckout(@NonNull Activity activity, Checkout checkout) {
        VcnCheckoutActivity.startActivity(activity, VCN_CHECKOUT_REQUEST, checkout);
    }

    public enum Environment {
        SANDBOX("sandbox.affirm.com", "tracker.affirm.com"),
        PRODUCTION("api-cf.affirm.com", "tracker.affirm.com");

        final String baseUrl;
        final String trackerBaseUrl;

        Environment(String baseUrl, String trackerBaseUrl) {
            this.baseUrl = baseUrl;
            this.trackerBaseUrl = trackerBaseUrl;
        }

        @Override
        public String toString() {
            return "Environment{" + baseUrl + ", " + trackerBaseUrl + '}';
        }
    }

    public static final class Configuration {
        final String publicKey;
        final Environment environment;
        final String name;

        Configuration(Builder builder) {
            this.publicKey = builder.publicKey;
            this.environment = builder.environment;
            this.name = builder.name;
        }

        public static final class Builder {
            private String publicKey;
            private Environment environment = Environment.SANDBOX;
            private String name;

            public Builder() {
            }

            public Builder setPublicKey(@NonNull String publicKey) {
                this.publicKey = publicKey;
                return this;
            }

            public Builder setEnvironment(@NonNull Environment environment) {
                this.environment = environment;
                return this;
            }

            /**
             * Sets the level of logging to display, where each level includes all those below it. The default
             * level is {@link #LOG_LEVEL_NONE}. Please ensure this is set to {@link #LOG_LEVEL_ERROR}
             * or {@link #LOG_LEVEL_NONE} before deploying your app to ensure no sensitive information is
             * logged. The levels are:
             * <ul>
             * <li>{@link #LOG_LEVEL_VERBOSE}</li>
             * <li>{@link #LOG_LEVEL_DEBUG}</li>
             * <li>{@link #LOG_LEVEL_INFO}</li>
             * <li>{@link #LOG_LEVEL_WARNING}</li>
             * <li>{@link #LOG_LEVEL_ERROR}</li>
             * <li>{@link #LOG_LEVEL_NONE}</li>
             * </ul>
             *
             * @param logLevel The level of logcat logging that Affirm should do.
             */
            public Builder setLogLevel(int logLevel) {
                AffirmLog.setLogLevel(logLevel);
                return this;
            }

            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            public Configuration build() {
                if (publicKey == null) {
                    throw new IllegalArgumentException("public key cannot be null");
                }

                return new Configuration(this);
            }
        }
    }
}

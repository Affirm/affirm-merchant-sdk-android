package com.affirm.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.affirm.android.ModalActivity.ModalType.PRODUCT;
import static com.affirm.android.ModalActivity.ModalType.SITE;

public final class Affirm {

    private Affirm() {
    }

    public static final int LOG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int LOG_LEVEL_DEBUG = Log.DEBUG;
    public static final int LOG_LEVEL_INFO = Log.INFO;
    public static final int LOG_LEVEL_WARNING = Log.WARN;
    public static final int LOG_LEVEL_ERROR = Log.ERROR;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;

    private static final int CHECKOUT_REQUEST = 8076;
    private static final int VCN_CHECKOUT_REQUEST = 8077;

    public interface CheckoutCallbacks {
        void onAffirmCheckoutError(@Nullable String message);

        void onAffirmCheckoutCancelled();

        void onAffirmCheckoutSuccess(@NonNull String token);
    }

    public interface VcnCheckoutCallbacks {
        void onAffirmVcnCheckoutError(@Nullable String message);

        void onAffirmVcnCheckoutCancelled();

        void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails);
    }

    public interface PromoCallback {
        void onFailure(Throwable throwable);
    }

    /**
     * Returns the level of logging that will be displayed.
     */
    public static int getLogLevel() {
        return AffirmLog.getLogLevel();
    }

    public static void initialize(Configuration configuration) {
        if (isInitialized()) {
            AffirmLog.w("Affirm is already initialized");
            return;
        }
        AffirmPlugins.initialize(configuration);
    }

    private static boolean isInitialized() {
        return AffirmPlugins.get() != null;
    }

    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout) {
        CheckoutActivity.startActivity(activity, CHECKOUT_REQUEST, checkout);
    }

    public static void startVcnCheckout(@NonNull Activity activity, Checkout checkout) {
        VcnCheckoutActivity.startActivity(activity, VCN_CHECKOUT_REQUEST, checkout);
    }

    private static void startPrequalFlow(@NonNull Context context, float amount,
                                         @Nullable String promoId) {
        PrequalActivity.startActivity(context, amount, promoId);
    }

    public static void startSiteModal(@NonNull Context context, @Nullable String modalId) {
        ModalActivity.startActivity(context, 0f, SITE, modalId);
    }

    public static void startProductModal(@NonNull Context context, float amount,
                                         @Nullable String modalId) {
        ModalActivity.startActivity(context, amount, PRODUCT, modalId);
    }

    public static void writePromo(@NonNull Context context,
                                  @NonNull final AffirmPromoLabel promoLabel,
                                  @Nullable final String promoId,
                                  final float amount,
                                  final boolean showCta,
                                  @Nullable final PromoCallback promoCallback) {
        final SpannablePromoCallback callback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull final String promo, final boolean showPrequal) {
                promoLabel.setTag(showPrequal);
                promoLabel.setLabel(promo);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                if (promoCallback != null) {
                    promoCallback.onFailure(exception);
                }
            }
        };

        final PromoRequest affirmPromoRequest =
                new PromoRequest(promoId, amount, showCta, callback);
        promoLabel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                affirmPromoRequest.create();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                affirmPromoRequest.cancel();
                promoLabel.removeOnAttachStateChangeListener(this);
            }
        });

        final WeakReference contextRef = new WeakReference<>(context);
        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = (Context) contextRef.get();
                if (context == null) {
                    return;
                }
                boolean showPrequal = (boolean) v.getTag();
                if (showPrequal) {
                    startPrequalFlow(context, amount, promoId);
                } else {
                    startProductModal(context, amount, null);
                }
            }
        };
        promoLabel.setOnClickListener(onClickListener);
    }

    public static boolean handleCheckoutData(@NonNull CheckoutCallbacks callbacks,
                                             int requestCode,
                                             int resultCode,
                                             @Nullable Intent data) {
        if (data == null) {
            return false;
        }
        if (requestCode == CHECKOUT_REQUEST) {
            switch (resultCode) {
                case RESULT_OK:
                    callbacks.onAffirmCheckoutSuccess(
                            data.getStringExtra(CheckoutActivity.CHECKOUT_TOKEN));
                    break;
                case RESULT_CANCELED:
                    callbacks.onAffirmCheckoutCancelled();
                    break;
                case CheckoutCommonActivity.RESULT_ERROR:
                    callbacks.onAffirmCheckoutError(
                            data.getStringExtra(CheckoutActivity.CHECKOUT_ERROR));
                    break;
                default:
            }

            return true;
        }

        return false;
    }

    public static boolean handleVcnCheckoutData(@NonNull VcnCheckoutCallbacks callbacks,
                                                int requestCode,
                                                int resultCode,
                                                @Nullable Intent data) {
        if (data == null) {
            return false;
        }
        if (requestCode == VCN_CHECKOUT_REQUEST) {
            switch (resultCode) {
                case RESULT_OK:
                    callbacks.onAffirmVcnCheckoutSuccess(
                            (CardDetails) data.getParcelableExtra(
                                    VcnCheckoutActivity.CREDIT_DETAILS));
                    break;
                case RESULT_CANCELED:
                    callbacks.onAffirmVcnCheckoutCancelled();
                    break;
                case CheckoutCommonActivity.RESULT_ERROR:
                    callbacks.onAffirmVcnCheckoutError(
                            data.getStringExtra(VcnCheckoutActivity.CHECKOUT_ERROR));
                    break;
                default:
            }

            return true;
        }

        return false;
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
             * Sets the level of logging to display, where each level includes all those below it
             * . The default
             * level is {@link #LOG_LEVEL_NONE}. Please ensure this is set to
             * {@link #LOG_LEVEL_ERROR}
             * or {@link #LOG_LEVEL_NONE} before deploying your app to ensure no sensitive
             * information is
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

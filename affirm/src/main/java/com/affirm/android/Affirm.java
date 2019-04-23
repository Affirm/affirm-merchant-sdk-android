package com.affirm.android;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.affirm.android.AffirmConstants.CHECKOUT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_TOKEN;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.PRODUCTION_JS_URL;
import static com.affirm.android.AffirmConstants.PRODUCTION_URL;
import static com.affirm.android.AffirmConstants.SANDBOX_JS_URL;
import static com.affirm.android.AffirmConstants.SANDBOX_URL;
import static com.affirm.android.AffirmConstants.TRACKER_URL;
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
    private static final int PREQUAL_REQUEST = 8078;
    static final int RESULT_ERROR = -8575;

    private static final String LIFE_FRAGMENT_TAG = "LifeFragmentTag";

    public interface PrequalCallbacks {
        void onAffirmPrequalError(@Nullable String message);
    }

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

    public enum Environment {
        SANDBOX(SANDBOX_URL, SANDBOX_JS_URL, TRACKER_URL),
        PRODUCTION(PRODUCTION_URL, PRODUCTION_JS_URL, TRACKER_URL);

        final String baseUrl;
        final String trackerBaseUrl;
        final String jsUrl;

        Environment(String baseUrl, String jsUrl, String trackerBaseUrl) {
            this.baseUrl = baseUrl;
            this.jsUrl = jsUrl;
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
        final String merchantName;

        Configuration(Builder builder) {
            this.publicKey = builder.publicKey;
            this.environment = builder.environment;
            this.merchantName = builder.merchantName;
        }

        public static final class Builder {
            private final String publicKey;
            private final Environment environment;
            private String merchantName;

            /**
             * @param publicKey   Set the public key to be used by Affirm.
             * @param environment Set the environment to be used by Affirm.
             */
            public Builder(@NonNull String publicKey, @NonNull Environment environment) {
                this.publicKey = publicKey;
                this.environment = environment;
            }

            /**
             * Set the level of logging to display. The default level is {@link #LOG_LEVEL_NONE}.
             * <p>
             * Please ensure this is set to {@link #LOG_LEVEL_ERROR} or {@link #LOG_LEVEL_NONE}
             * before deploying your app.
             * <p>
             * The levels are:
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
             * @return The same builder, for easy chaining.
             */
            public Builder setLogLevel(int logLevel) {
                AffirmLog.setLogLevel(logLevel);
                return this;
            }

            /**
             * Set your proud partnership name, it's optional
             *
             * @param merchantName your proud partnership name
             * @return The same builder, for easy chaining.
             */
            public Builder setMerchantName(@Nullable String merchantName) {
                this.merchantName = merchantName;
                return this;
            }

            /**
             * Construct this builder into a concrete {@code Configuration} instance.
             *
             * @return A constructed {@code Configuration} object.
             */
            public Configuration build() {
                AffirmUtils.requireNonNull(publicKey, "public key cannot be null");
                AffirmUtils.requireNonNull(environment, "environment cannot be null");
                return new Configuration(this);
            }
        }
    }

    /**
     * Returns the level of logging that will be displayed.
     */
    public static int getLogLevel() {
        return AffirmLog.getLogLevel();
    }

    public static void initialize(@NonNull Configuration configuration) {
        AffirmUtils.requireNonNull(configuration, "configuration cannot be null");

        if (isInitialized()) {
            AffirmLog.w("Affirm is already initialized");
            return;
        }
        AffirmPlugins.initialize(configuration);
    }

    private static boolean isInitialized() {
        return AffirmPlugins.get() != null;
    }


    /**
     * Start track order
     *
     * @param activity    activity {@link Activity}
     * @param affirmTrack AffirmTrack object that containers order & product info
     */
    public static void trackOrderConfirmed(@NonNull final Activity activity,
                                           @NonNull AffirmTrack affirmTrack) {
        final ViewGroup container =
                activity.getWindow().getDecorView().findViewById(android.R.id.content);
        AffirmTrackView affirmTrackView = new AffirmTrackView(activity, affirmTrack,
                new AffirmTrackView.AffirmTrackCallback() {

                    @Override
                    public void onSuccess(AffirmTrackView affirmTrackView) {
                        AffirmLog.d("Track successfully");
                        container.removeView(affirmTrackView);
                    }

                    @Override
                    public void onFailed(AffirmTrackView affirmTrackView, String reason) {
                        AffirmLog.e("Track Failed: " + reason);
                        container.removeView(affirmTrackView);
                    }
                });
        container.addView(affirmTrackView);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param useVCN   Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout,
                                     boolean useVCN) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        AffirmUtils.requireNonNull(checkout, "checkout cannot be null");
        if (useVCN) {
            VcnCheckoutActivity.startActivity(activity, VCN_CHECKOUT_REQUEST, checkout);
        } else {
            CheckoutActivity.startActivity(activity, CHECKOUT_REQUEST, checkout);
        }
    }

    /**
     * Start site modal
     *
     * @param activity activity {@link Activity}
     * @param modalId  the client's modal id
     */
    public static void showSiteModal(@NonNull Activity activity, @Nullable String modalId) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        ModalActivity.startActivity(activity, 0, 0f, SITE, modalId);
    }

    /**
     * Start product modal
     *
     * @param activity activity {@link Activity}
     * @param amount   (Float) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     */
    public static void showProductModal(@NonNull Activity activity, float amount,
                                        @Nullable String modalId) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        ModalActivity.startActivity(activity, 0, amount, PRODUCT, modalId);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's modal id
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final String promoId,
                                           final float amount,
                                           final boolean showCta) {
        AffirmUtils.requireNonNull(promotionButton, "AffirmPromotionButton cannot be null");
        final SpannablePromoCallback callback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull final String promo, final boolean showPrequal) {
                promotionButton.setTag(showPrequal);
                promotionButton.setLabel(promo);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                AffirmLog.e(exception.toString());
            }
        };

        final PromoRequest affirmPromoRequest =
                new PromoRequest(promoId, amount, showCta, callback);

        final LifecycleListener lifecycleListener = new LifecycleListener() {
            @Override
            public void onStart() {
                affirmPromoRequest.create();
            }

            @Override
            public void onStop() {
                affirmPromoRequest.cancel();
            }
        };

        promotionButton.setTag(UUID.randomUUID());
        promotionButton.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(final View v) {
                Activity activity = AffirmUtils.getActivityFromView(v);
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                LifeListenerFragment fragment =
                        getLifeListenerFragment(activity, LIFE_FRAGMENT_TAG + v.getTag());
                fragment.addLifeListener(lifecycleListener);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                promotionButton.removeOnAttachStateChangeListener(this);
                Activity activity = AffirmUtils.getActivityFromView(v);
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                LifeListenerFragment fragment =
                        getLifeListenerFragment(activity, LIFE_FRAGMENT_TAG + v.getTag());
                fragment.removeLifeListener();
            }
        });

        final View.OnClickListener onClickListener = v -> {
            Activity activity = AffirmUtils.getActivityFromView(v);
            if (activity == null || TextUtils.isEmpty(promotionButton.getText())) {
                return;
            }
            boolean showPrequal = (boolean) v.getTag();
            if (showPrequal) {
                PrequalActivity.startActivity(activity,
                        PREQUAL_REQUEST, amount, promoId);
            } else {
                ModalActivity.startActivity(activity,
                        PREQUAL_REQUEST, amount, PRODUCT, null);
            }
        };
        promotionButton.setOnClickListener(onClickListener);
    }

    // Add a blank fragment to handle the lifecycle of the activity
    private static LifeListenerFragment getLifeListenerFragment(Activity activity, String tag) {
        final FragmentManager manager = activity.getFragmentManager();
        LifeListenerFragment fragment =
                (LifeListenerFragment) manager.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new LifeListenerFragment();
            manager
                    .beginTransaction()
                    .add(fragment, tag)
                    .commitAllowingStateLoss();
        }
        return fragment;
    }

    /**
     * Helper method to get the Result from prequal
     */
    public static boolean handlePrequalData(@NonNull PrequalCallbacks callbacks,
                                            int requestCode,
                                            int resultCode,
                                            @Nullable Intent data) {
        AffirmUtils.requireNonNull(callbacks, "PrequalCallbacks cannot be null");

        if (requestCode == PREQUAL_REQUEST) {
            switch (resultCode) {
                case RESULT_ERROR:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmPrequalError(data.getStringExtra(CHECKOUT_ERROR));
                    break;
                default:
                    break;
            }

            return true;
        }

        return false;
    }

    /**
     * Helper method to get the Result from the `startCheckout`
     */
    public static boolean handleCheckoutData(@NonNull CheckoutCallbacks callbacks,
                                             int requestCode,
                                             int resultCode,
                                             @Nullable Intent data) {
        AffirmUtils.requireNonNull(callbacks, "CheckoutCallbacks cannot be null");

        if (requestCode == CHECKOUT_REQUEST) {
            switch (resultCode) {
                case RESULT_OK:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmCheckoutSuccess(data.getStringExtra(CHECKOUT_TOKEN));
                    break;
                case RESULT_CANCELED:
                    callbacks.onAffirmCheckoutCancelled();
                    break;
                case RESULT_ERROR:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmCheckoutError(data.getStringExtra(CHECKOUT_ERROR));
                    break;
                default:
                    break;
            }

            return true;
        }

        return false;
    }

    /**
     * Helper method to get the Result from the `startVcnCheckout`
     */
    public static boolean handleVcnCheckoutData(@NonNull VcnCheckoutCallbacks callbacks,
                                                int requestCode,
                                                int resultCode,
                                                @Nullable Intent data) {
        AffirmUtils.requireNonNull(callbacks, "VcnCheckoutCallbacks cannot be null");

        if (requestCode == VCN_CHECKOUT_REQUEST) {
            switch (resultCode) {
                case RESULT_OK:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmVcnCheckoutSuccess(
                            (CardDetails) data.getParcelableExtra(CREDIT_DETAILS));
                    break;
                case RESULT_CANCELED:
                    callbacks.onAffirmVcnCheckoutCancelled();
                    break;
                case RESULT_ERROR:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmVcnCheckoutError(data.getStringExtra(CHECKOUT_ERROR));
                    break;
                default:
                    break;
            }

            return true;
        }

        return false;
    }
}

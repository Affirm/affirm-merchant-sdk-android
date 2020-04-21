package com.affirm.android;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.VcnReason;

import java.math.BigDecimal;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmConstants.CHECKOUT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_TOKEN;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.VCN_REASON;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO;
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

    // these values are set by the builder
    private static int checkoutRequest;
    private static int vcnCheckoutRequest;
    private static int prequalRequest;
    private static String receiveReasonCodes;

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

        void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason);

        void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails);
    }

    public enum Location {
        US, CA
    }

    public enum Environment {
        SANDBOX,
        PRODUCTION;

        String baseUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.getSandboxUrl();
                default:
                    return AffirmConstants.getProductionUrl();
            }
        }

        String baseJsUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.getSandboxJsUrl();
                default:
                    return AffirmConstants.getProductionJsUrl();
            }
        }

        String trackerBaseUrl() {
            return AffirmConstants.getTrackerUrl();
        }

        String basePromoUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.getStagingPromoUrl();
                default:
                    return AffirmConstants.getProductionPromoUrl();
            }
        }

        String baseInvalidCheckoutRedirectUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.getStagingInvalidCheckoutRedirectUrl();
                default:
                    return AffirmConstants.getProductionInvalidCheckoutRedirectUrl();
            }
        }
    }

    public static final class Configuration {
        final String publicKey;
        final Environment environment;
        final String merchantName;

        Configuration(Builder builder) {
            this.publicKey = builder.publicKey;
            this.merchantName = builder.merchantName;

            if (builder.environment != null) {
                this.environment = builder.environment;
            } else {
                this.environment = Environment.PRODUCTION;
            }

            if (builder.receiveReasonCodes != null) {
                receiveReasonCodes = builder.receiveReasonCodes;
            } else {
                receiveReasonCodes = "false";
            }

            if (builder.checkoutRequestCode != 0) {
                checkoutRequest = builder.checkoutRequestCode;
            } else {
                checkoutRequest = 8076;
            }

            if (builder.vcnCheckoutRequestCode != 0) {
                vcnCheckoutRequest = builder.vcnCheckoutRequestCode;
            } else {
                vcnCheckoutRequest = 8077;
            }

            if (builder.prequalRequestCode != 0) {
                prequalRequest = builder.prequalRequestCode;
            } else {
                prequalRequest = 8078;
            }
        }

        public static final class Builder {
            private final String publicKey;
            private Environment environment;
            private String merchantName;
            private int checkoutRequestCode;
            private int vcnCheckoutRequestCode;
            private int prequalRequestCode;
            private String receiveReasonCodes;

            /**
             * @param publicKey Set the public key to be used by Affirm.
             */
            public Builder(@NonNull String publicKey) {
                this.publicKey = publicKey;
            }

            /**
             * @param publicKey   Set the public key to be used by Affirm.
             * @param environment Set the environment to be used by Affirm.
             */
            public Builder(@NonNull String publicKey, @Nullable Environment environment) {
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
             * Set the environment to be used by Affirm, it's optional
             *
             * @param environment your environment to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setEnvironment(@NonNull Environment environment) {
                this.environment = environment;
                return this;
            }

            /**
             * Set the checkout request code to be used by Affirm, it's optional
             *
             * @param receiveReasonCodes receive reason codes when a checkout is canceled
             * @return The same builder, for easy chaining.
             */
            public Builder setReceiveReasonCodes(@Nullable String receiveReasonCodes) {
                this.receiveReasonCodes = receiveReasonCodes;
                return this;
            }

            /**
             * Set the checkout request code to be used by Affirm, it's optional
             *
             * @param checkoutRequestCode your checkout request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setCheckoutRequestCode(int checkoutRequestCode) {
                this.checkoutRequestCode = checkoutRequestCode;
                return this;
            }

            /**
             * Set the vcn checkout request code to be used by Affirm, it's optional
             *
             * @param vcnCheckoutRequestCode your vcn checkout request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setVcnCheckoutRequestCode(int vcnCheckoutRequestCode) {
                this.vcnCheckoutRequestCode = vcnCheckoutRequestCode;
                return this;
            }

            /**
             * Set the prequal request code to be used by Affirm, it's optional
             *
             * @param prequalRequestCode your prequal request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setPrequalRequestCode(int prequalRequestCode) {
                this.prequalRequestCode = prequalRequestCode;
                return this;
            }

            public Builder setLocation(Location location) {
                AffirmConstants.setLocation(location);
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

    public static final class PromoRequestData {
        @Nullable
        private String promoId;

        @Nullable
        private PromoPageType pageType;

        private BigDecimal amount;
        private boolean showCta;
        @NonNull
        private AffirmColor affirmColor;
        @NonNull
        private AffirmLogoType affirmLogoType;

        private PromoRequestData(
                @Nullable String promoId,
                @Nullable PromoPageType pageType,
                BigDecimal amount,
                boolean showCta,
                @NonNull AffirmColor affirmColor,
                @NonNull AffirmLogoType affirmLogoType
        ) {
            this.promoId = promoId;
            this.pageType = pageType;
            this.amount = amount;
            this.showCta = showCta;
            this.affirmColor = affirmColor;
            this.affirmLogoType = affirmLogoType;
        }

        @Nullable
        String getPromoId() {
            return promoId;
        }

        @Nullable
        PromoPageType getPageType() {
            return pageType;
        }

        BigDecimal getAmount() {
            return amount;
        }

        boolean showCta() {
            return showCta;
        }

        @NonNull
        AffirmColor getAffirmColor() {
            return affirmColor;
        }

        @NonNull
        AffirmLogoType getAffirmLogoType() {
            return affirmLogoType;
        }

        public static final class Builder {
            @Nullable
            private String promoId;
            @Nullable
            private PromoPageType pageType;
            private BigDecimal amount;
            private boolean showCta;
            private AffirmColor affirmColor;
            private AffirmLogoType affirmLogoType;

            /**
             * @param amount  a float that represents the amount to retrieve pricing for
             *                eg 112.02 as $112 and 2¢
             * @param showCta whether need to show cta
             */
            public Builder(BigDecimal amount, boolean showCta) {
                this.amount = amount;
                this.showCta = showCta;
            }

            /**
             * @param promoId the client's modal id, it's optional
             */
            public PromoRequestData.Builder setPromoId(@Nullable String promoId) {
                this.promoId = promoId;
                return this;
            }

            /**
             * @param pageType must be one of "banner, cart, category, homepage, landing,
             *                 payment, product, search", it's optional
             */
            public PromoRequestData.Builder setPageType(@Nullable PromoPageType pageType) {
                this.pageType = pageType;
                return this;
            }

            /**
             * @param affirmColor the color used for the affirm logo in the response, it's optional
             */
            public PromoRequestData.Builder setAffirmColor(@NonNull AffirmColor affirmColor) {
                this.affirmColor = affirmColor;
                return this;
            }

            /**
             * @param logoType the type of affirm logo to use in the response, it's optional
             */
            public PromoRequestData.Builder setAffirmLogoType(@NonNull AffirmLogoType logoType) {
                this.affirmLogoType = logoType;
                return this;
            }

            public PromoRequestData build() {
                if (affirmLogoType == null) {
                    affirmLogoType = AFFIRM_DISPLAY_TYPE_LOGO;
                }

                if (affirmColor == null) {
                    affirmColor = AFFIRM_COLOR_TYPE_BLUE;
                }

                return new PromoRequestData(
                        promoId,
                        pageType,
                        amount,
                        showCta,
                        affirmColor,
                        affirmLogoType
                );
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
            VcnCheckoutActivity.startActivity(
                    activity, vcnCheckoutRequest, checkout, receiveReasonCodes);
        } else {
            CheckoutActivity.startActivity(activity, checkoutRequest, checkout);
        }
    }

    /**
     * Start site modal
     *
     * @param activity activity {@link Activity}
     * @param modalId  the client's modal id
     */
    public static void showSiteModal(@NonNull Activity activity, @Nullable String modalId) {
        showSiteModal(activity, modalId, null, null);
    }

    /**
     * Start site modal
     *
     * @param activity activity {@link Activity}
     * @param modalId  the client's modal id
     * @param pageType need to use one of "banner, cart, category, homepage, landing,
     *                 payment, product, search"
     */
    public static void showSiteModal(@NonNull Activity activity, @Nullable String modalId,
                                     @Nullable PromoPageType pageType, @Nullable String promoId) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        ModalActivity.startActivity(activity, 0, BigDecimal.valueOf(0.0), SITE, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Start product modal
     *
     * @param activity activity {@link Activity}
     * @param amount   (Float) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     */
    public static void showProductModal(@NonNull Activity activity, BigDecimal amount,
                                        @Nullable String modalId) {
        showProductModal(activity, amount, modalId, null, null);
    }

    /**
     * Start product modal
     *
     * @param activity activity {@link Activity}
     * @param amount   (Float) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     * @param pageType need to use one of "banner, cart, category, homepage, landing,
     *                 payment, product, search"
     */
    public static void showProductModal(@NonNull Activity activity,
                                        BigDecimal amount,
                                        @Nullable String modalId,
                                        @Nullable PromoPageType pageType,
                                        @Nullable String promoId) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        ModalActivity.startActivity(activity, 0, amount, PRODUCT, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           final BigDecimal amount,
                                           final boolean showCta) {
        configureWithAmount(promotionButton, null, null, amount, showCta);
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
                                           final BigDecimal amount,
                                           final boolean showCta) {
        configureWithAmount(promotionButton, promoId, null, amount, showCta);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final PromoPageType pageType,
                                           final BigDecimal amount,
                                           final boolean showCta) {
        configureWithAmount(promotionButton, null, pageType, amount, showCta);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's modal id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final String promoId,
                                           @Nullable final PromoPageType pageType,
                                           final BigDecimal amount,
                                           final boolean showCta) {
        AffirmUtils.requireNonNull(promotionButton, "AffirmPromotionButton cannot be null");
        final SpannablePromoCallback callback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull final String promoMessage,
                                       final boolean showPrequal) {
                promotionButton.setTag(showPrequal);
                promotionButton.setLabel(promoMessage);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                AffirmLog.e(exception.toString());
                promotionButton.setVisibility(View.GONE);
            }
        };

        final PromoRequest affirmPromoRequest =
                new PromoRequest(promoId, pageType, amount, showCta,
                        promotionButton.getAffirmColor(),
                        promotionButton.getAffirmLogoType(),
                        promotionButton.isHtmlStyle(),
                        callback);

        final LifecycleListener lifecycleListener = new LifecycleListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onStop() {

            }

            @Override
            public void onDestroy() {
                affirmPromoRequest.cancel();
            }
        };

        promotionButton.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(final View v) {
                Activity activity = AffirmUtils.getActivityFromView(v);
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                LifeListenerFragment fragment =
                        getLifeListenerFragment(activity);
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
                        getLifeListenerFragment(activity);
                fragment.removeLifeListener(lifecycleListener);
            }
        });

        affirmPromoRequest.create();

        final View.OnClickListener onClickListener = v -> {
            Activity activity = AffirmUtils.getActivityFromView(v);
            if (activity == null || promotionButton.isEmpty()) {
                return;
            }
            boolean showPrequal = (boolean) v.getTag();
            String type = pageType != null ? pageType.getType() : null;
            if (showPrequal) {
                PrequalActivity.startActivity(activity,
                        prequalRequest, amount, promoId, type);
            } else {
                ModalActivity.startActivity(activity,
                        prequalRequest, amount, PRODUCT, null,
                        type, promoId);
            }
        };
        promotionButton.setOnClickListener(onClickListener);
    }

    /**
     * Fetch promotional message, you can display it yourself
     *
     * @param requestData a class containing data about the request to make
     * @param textSize    the textSize for the span
     * @param context     the context being used
     * @param callback    a class that's called when the request completes
     */
    public static AffirmRequest fetchPromotion(
            @NonNull PromoRequestData requestData,
            float textSize,
            @NonNull Context context,
            @NonNull final PromotionCallback callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promo,
                                       boolean showPrequal) {
                callback.onSuccess(
                        AffirmUtils.createSpannableForText(
                                promo,
                                textSize,
                                requestData.getAffirmLogoType(),
                                requestData.getAffirmColor(),
                                context
                        ),
                        showPrequal
                );
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                callback.onFailure(exception);
            }
        };
        return buildPromoRequest(requestData, promoCallback, false);
    }

    /**
     * Fetch promotional html message, you can display it yourself
     *
     * @param requestData a class containing data about the request to make
     * @param callback    a class that's called when the request completes
     */
    public static AffirmRequest fetchHtmlPromotion(
            @NonNull PromoRequestData requestData,
            @NonNull final HtmlPromotionCallback callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promo,
                                       boolean showPrequal) {
                callback.onSuccess(promo, showPrequal);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                callback.onFailure(exception);
            }
        };
        return buildPromoRequest(requestData, promoCallback, true);
    }

    private static PromoRequest buildPromoRequest(@NonNull PromoRequestData requestData,
                            SpannablePromoCallback promoCallback, Boolean isHtmlStyle) {
        return new PromoRequest(
                requestData.getPromoId(),
                requestData.getPageType(),
                requestData.getAmount(),
                requestData.showCta(),
                requestData.getAffirmColor(),
                requestData.getAffirmLogoType(),
                isHtmlStyle,
                promoCallback
        );
    }

    /**
     * Handling events that click on the promotion message
     *
     * @param activity          activity {@link Activity}
     * @param promoRequestModal a class contains the parameters required for the request
     * @param showPrequal       This value comes from the callback of the method `fetchPromotion`
     */
    public static void onPromotionClick(@NonNull Activity activity,
                                        @NonNull PromoRequestData promoRequestModal,
                                        boolean showPrequal) {
        PromoPageType pageType = promoRequestModal.getPageType();
        String type = pageType != null ? pageType.getType() : null;
        if (showPrequal) {
            PrequalActivity.startActivity(activity, prequalRequest,
                    promoRequestModal.getAmount(),
                    promoRequestModal.getPromoId(),
                    type);
        } else {
            ModalActivity.startActivity(activity,
                    prequalRequest, promoRequestModal.getAmount(), PRODUCT, null,
                    type, promoRequestModal.getPromoId());
        }
    }

    // Add a blank fragment to handle the lifecycle of the activity
    private static LifeListenerFragment getLifeListenerFragment(Activity activity) {
        final FragmentManager manager = activity.getFragmentManager();
        LifeListenerFragment fragment =
                (LifeListenerFragment) manager.findFragmentByTag(LIFE_FRAGMENT_TAG);
        if (fragment == null) {
            fragment = new LifeListenerFragment();
            manager
                    .beginTransaction()
                    .add(fragment, LIFE_FRAGMENT_TAG)
                    .commitAllowingStateLoss();
            manager.executePendingTransactions();
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

        if (requestCode == prequalRequest) {
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

        if (requestCode == checkoutRequest) {
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

        if (requestCode == vcnCheckoutRequest) {
            switch (resultCode) {
                case RESULT_OK:
                    AffirmUtils.requireNonNull(data);
                    callbacks.onAffirmVcnCheckoutSuccess(
                            (CardDetails) data.getParcelableExtra(CREDIT_DETAILS));
                    break;
                case RESULT_CANCELED:
                    if (receiveReasonCodes.equals("false")) {

                        callbacks.onAffirmVcnCheckoutCancelled();
                    } else {

                        if (data == null) {
                            data = new Intent();
                            VcnReason reason = VcnReason.builder().setReason("canceled").build();
                            data.putExtra(VCN_REASON, reason);
                        }

                        callbacks.onAffirmVcnCheckoutCancelledReason(
                                (VcnReason) data.getParcelableExtra(VCN_REASON));
                    }
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

package com.affirm.android;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.AffirmTrack;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.CardDetailsInner;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.VcnReason;
import com.google.gson.JsonObject;

import org.joda.money.Money;

import java.math.BigDecimal;
import java.util.List;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmConstants.AFFIRM_NOT_INITIALIZED_MESSAGE;
import static com.affirm.android.AffirmConstants.CHECKOUT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_TOKEN;
import static com.affirm.android.AffirmConstants.COUNTY_CODE_CAN;
import static com.affirm.android.AffirmConstants.COUNTY_CODE_UK;
import static com.affirm.android.AffirmConstants.COUNTY_CODE_USA;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.LOCALE_USA;
import static com.affirm.android.AffirmConstants.VCN_REASON;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO;
import static com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_WEBVIEW_CLICK;
import static com.affirm.android.AffirmTracker.TrackingEvent.VCN_CHECKOUT_CREATION_CLICK;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;
import static com.affirm.android.AffirmTracker.createTrackingCheckout;
import static com.affirm.android.ModalFragment.ModalType.PRODUCT;
import static com.affirm.android.ModalFragment.ModalType.SITE;

public final class Affirm {

    private Affirm() {
    }

    public static final int LOG_LEVEL_VERBOSE = Log.VERBOSE;
    public static final int LOG_LEVEL_DEBUG = Log.DEBUG;
    public static final int LOG_LEVEL_INFO = Log.INFO;
    public static final int LOG_LEVEL_WARNING = Log.WARN;
    public static final int LOG_LEVEL_ERROR = Log.ERROR;
    public static final int LOG_LEVEL_NONE = Integer.MAX_VALUE;

    private static final int DEFAULT_CHECKOUT_REQUEST = 8076;
    private static final int DEFAULT_VCN_CHECKOUT_REQUEST = 8077;
    private static final int DEFAULT_PREQUAL_REQUEST = 8078;
    private static final String DEFAULT_RECEIVE_REASON_CODES = "false";

    // these values are set by the builder
    private static int checkoutRequest = DEFAULT_CHECKOUT_REQUEST;
    private static int vcnCheckoutRequest = DEFAULT_VCN_CHECKOUT_REQUEST;
    private static int prequalRequest = DEFAULT_PREQUAL_REQUEST;
    private static String receiveReasonCodes = DEFAULT_RECEIVE_REASON_CODES;

    static final int RESULT_ERROR = -8575;
    static final int RESULT_CHECKOUT_CANCEL = -8576;

    private static final String LIFE_FRAGMENT_TAG = "LifeFragmentTag";

    private static void setCheckoutRequest(int checkoutRequest) {
        Affirm.checkoutRequest = checkoutRequest != 0
                ? checkoutRequest
                : DEFAULT_CHECKOUT_REQUEST;
    }

    private static void setVcnCheckoutRequest(int vcnCheckoutRequest) {
        Affirm.vcnCheckoutRequest = vcnCheckoutRequest != 0
                ? vcnCheckoutRequest
                : DEFAULT_VCN_CHECKOUT_REQUEST;
    }

    private static void setPrequalRequest(int prequalRequest) {
        Affirm.prequalRequest = prequalRequest != 0
                ? prequalRequest
                : DEFAULT_PREQUAL_REQUEST;
    }

    private static void setReceiveReasonCodes(@Nullable String receiveReasonCodes) {
        Affirm.receiveReasonCodes = receiveReasonCodes != null
                ? receiveReasonCodes
                : DEFAULT_RECEIVE_REASON_CODES;
    }

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

    public enum Environment {
        SANDBOX,
        PRODUCTION;

        String checkoutUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.SANDBOX_CHECKOUT_URL;
                default:
                    return AffirmConstants.PRODUCTION_CHECKOUT_URL;
            }
        }

        String jsUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.SANDBOX_JS_URL;
                default:
                    return AffirmConstants.PRODUCTION_JS_URL;
            }
        }

        String trackerUrl(String countryCode) {
            switch (countryCode) {
                case COUNTY_CODE_CAN:
                    return AffirmConstants.TRACKER_CA_URL;
                case COUNTY_CODE_UK:
                    return AffirmConstants.TRACKER_UK_URL;
                default:
                    return AffirmConstants.TRACKER_US_URL;
            }
        }

        String promoUrl(String countryCode) {
            switch (countryCode) {
                case COUNTY_CODE_CAN:
                    switch (this) {
                        case SANDBOX:
                            return AffirmConstants.SANDBOX_PROMO_CA_URL;
                        default:
                            return AffirmConstants.PRODUCTION_PROMO_CA_URL;
                    }
                case COUNTY_CODE_UK:
                    switch (this) {
                        case SANDBOX:
                            return AffirmConstants.SANDBOX_PROMO_UK_URL;
                        default:
                            return AffirmConstants.PRODUCTION_PROMO_UK_URL;
                    }
                default:
                    switch (this) {
                        case SANDBOX:
                            return AffirmConstants.SANDBOX_PROMO_US_URL;
                        default:
                            return AffirmConstants.PRODUCTION_PROMO_US_URL;
                    }
            }
        }

        String invalidCheckoutRedirectUrl() {
            switch (this) {
                case SANDBOX:
                    return AffirmConstants.SANDBOX_INVALID_CHECKOUT_REDIRECT_URL;
                default:
                    return AffirmConstants.PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL;
            }
        }
    }

    public static final class Configuration {
        final String publicKey;
        final Environment environment;
        final String merchantName;
        final String cardTip;
        final String locale;
        final String countryCode;

        Configuration(Builder builder) {
            this.publicKey = builder.publicKey;
            this.merchantName = builder.merchantName;
            this.cardTip = builder.cardTip;
            this.locale = builder.locale;
            this.countryCode = builder.countryCode;

            if (builder.environment != null) {
                this.environment = builder.environment;
            } else {
                this.environment = Environment.PRODUCTION;
            }
        }

        public static final class Builder {
            private String publicKey;
            private Environment environment;
            private String merchantName;
            private String cardTip;
            // When a locale is not provided, the locale will default to en_US
            private String locale = LOCALE_USA;
            // When a country-code is not provided, the country-code will default to USA
            private String countryCode = COUNTY_CODE_USA;

            /**
             * @param configuration Set the configuration to be used by Affirm.
             */
            public Builder(@NonNull Configuration configuration) {
                this.publicKey = configuration.publicKey;
                this.environment = configuration.environment;
                this.merchantName = configuration.merchantName;
                this.cardTip = configuration.cardTip;
                this.locale = configuration.locale;
                this.countryCode = configuration.countryCode;
            }

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
             * @param publicKey Set the public key to be used by Affirm.
             * @return The same builder, for easy chaining.
             */
            public Builder setPublicKey(@NonNull String publicKey) {
                this.publicKey = publicKey;
                return this;
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
                Affirm.setReceiveReasonCodes(receiveReasonCodes);
                return this;
            }

            /**
             * Set the checkout request code to be used by Affirm, it's optional
             *
             * @param checkoutRequestCode your checkout request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setCheckoutRequestCode(int checkoutRequestCode) {
                Affirm.setCheckoutRequest(checkoutRequestCode);
                return this;
            }

            /**
             * Set the vcn checkout request code to be used by Affirm, it's optional
             *
             * @param vcnCheckoutRequestCode your vcn checkout request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setVcnCheckoutRequestCode(int vcnCheckoutRequestCode) {
                Affirm.setVcnCheckoutRequest(vcnCheckoutRequestCode);
                return this;
            }

            /**
             * Set the prequal request code to be used by Affirm, it's optional
             *
             * @param prequalRequestCode your prequal request code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setPrequalRequestCode(int prequalRequestCode) {
                Affirm.setPrequalRequest(prequalRequestCode);
                return this;
            }

            /**
             * Set the locale to be used by Affirm, it's optional
             *
             * @param locale your locale info to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setLocale(String locale) {
                this.locale = locale;
                return this;
            }

            /**
             * Set the country code to be used by Affirm, it's optional
             *
             * @param countryCode your country code to be used by Affirm
             * @return The same builder, for easy chaining.
             */
            public Builder setCountryCode(String countryCode) {
                this.countryCode = countryCode;
                return this;
            }

            /**
             * Allow an option to pass a text string and set that string to a text label below the
             * Virtual Card image
             *
             * @param cardTip the text want to show
             * @return The same builder, for easy chaining.
             */
            public Builder setCardTip(@Nullable String cardTip) {
                this.cardTip = cardTip;
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
        @Nullable
        private List<Item> items;

        private PromoRequestData(
                @Nullable String promoId,
                @Nullable PromoPageType pageType,
                BigDecimal amount,
                boolean showCta,
                @NonNull AffirmColor affirmColor,
                @NonNull AffirmLogoType affirmLogoType,
                @Nullable List<Item> items
        ) {
            this.promoId = promoId;
            this.pageType = pageType;
            this.amount = amount;
            this.showCta = showCta;
            this.affirmColor = affirmColor;
            this.affirmLogoType = affirmLogoType;
            this.items = items;
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

        @Nullable
        List<Item> getItems() {
            return items;
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
            @Nullable
            private List<Item> items;

            /**
             * @param amount  a BigDecimal that represents the amount to retrieve pricing for
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

            public PromoRequestData.Builder setItems(@Nullable List<Item> items) {
                this.items = items;
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
                        affirmLogoType,
                        items
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

    /**
     * You can switch the public key & merchant name after calling the initialize method
     *
     * @param publicKey    Set the public key to be used by Affirm.
     * @param merchantName Set the merchant name to be used by Affirm.
     */
    public static void setPublicKeyAndMerchantName(@NonNull String publicKey,
                                                   @Nullable String merchantName) {
        if (!isInitialized()) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        AffirmPlugins.get().setConfiguration(
                new Affirm.Configuration.Builder(AffirmPlugins.get().getConfiguration())
                        .setPublicKey(publicKey)
                        .setMerchantName(merchantName)
                        .build());
    }

    /**
     * You can switch the public key after calling the initialize method
     *
     * @param publicKey Set the public key to be used by Affirm.
     */
    public static void setPublicKey(@NonNull String publicKey) {
        if (!isInitialized()) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        AffirmPlugins.get().setConfiguration(
                new Affirm.Configuration.Builder(AffirmPlugins.get().getConfiguration())
                        .setPublicKey(publicKey)
                        .build());
    }

    /**
     * You can switch the merchant name after calling the initialize method
     *
     * @param merchantName Set the merchant name to be used by Affirm.
     */
    public static void setMerchantName(@Nullable String merchantName) {
        if (!isInitialized()) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        AffirmPlugins.get().setConfiguration(
                new Affirm.Configuration.Builder(AffirmPlugins.get().getConfiguration())
                        .setMerchantName(merchantName)
                        .build());
    }

    /**
     * Updates the country code used by Affirm after initialization.
     *
     * @param countryCode Set the country code to be used by Affirm. Must not be null or empty.
     */
    public static void setCountryCode(@NonNull String countryCode) {
        if (!isInitialized()) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        if (countryCode.isEmpty()) {
            AffirmLog.w("Country code is empty. Please provide a valid country code.");
            return;
        }

        AffirmPlugins.get().setConfiguration(
                new Affirm.Configuration.Builder(AffirmPlugins.get().getConfiguration())
                        .setCountryCode(countryCode)
                        .build());
    }

    /**
     * Updates the locale used by Affirm after initialization.
     *
     * @param locale Set the locale to be used by Affirm. Must not be null or empty.
     */
    public static void setLocale(@NonNull String locale) {
        if (!isInitialized()) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            return;
        }

        if (locale.isEmpty()) {
            AffirmLog.w("Locale is empty. Please provide a valid locale string.");
            return;
        }

        AffirmPlugins.get().setConfiguration(
                new Affirm.Configuration.Builder(AffirmPlugins.get().getConfiguration())
                        .setLocale(locale)
                        .build());
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
     * Start track order
     *
     * @param fragment    fragment {@link Fragment}
     * @param affirmTrack AffirmTrack object that containers order & product info
     */
    public static void trackOrderConfirmed(@NonNull final Fragment fragment,
                                           @NonNull AffirmTrack affirmTrack) {
        trackOrderConfirmed(fragment.requireActivity(), affirmTrack);
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
        startCheckout(activity, checkout, null, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param fragment fragment {@link Fragment}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param useVCN   Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Fragment fragment, @NonNull Checkout checkout,
                                     boolean useVCN) {
        startCheckout(fragment, checkout, null, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param activity       activity {@link Activity}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     * @param useVCN         Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout,
                                     int cardAuthWindow, boolean useVCN) {
        startCheckout(activity, checkout, null, cardAuthWindow, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param fragment       fragment {@link Fragment}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     * @param useVCN         Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Fragment fragment, @NonNull Checkout checkout,
                                     int cardAuthWindow, boolean useVCN) {
        startCheckout(fragment, checkout, null, cardAuthWindow, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     * @param useVCN   Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout,
                                     @Nullable String caas, boolean useVCN) {
        startCheckout(activity, checkout, caas, -1, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param fragment fragment {@link Fragment}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     * @param useVCN   Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Fragment fragment, @NonNull Checkout checkout,
                                     @Nullable String caas, boolean useVCN) {
        startCheckout(fragment, checkout, caas, -1, useVCN);
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param activity       activity {@link Activity}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param caas           caas merchant-level attribute
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     * @param useVCN         Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Activity activity, @NonNull Checkout checkout,
                                     @Nullable String caas, int cardAuthWindow, boolean useVCN) {
        AffirmUtils.requireNonNull(activity, "activity cannot be null");
        AffirmUtils.requireNonNull(checkout, "checkout cannot be null");
        JsonObject trackInfo = createTrackingCheckout(checkout, caas, cardAuthWindow, useVCN);
        if (useVCN) {
            AffirmTracker.track(VCN_CHECKOUT_CREATION_CLICK, INFO, trackInfo);
            startVcnCheckout(activity, checkout, caas, null, false, cardAuthWindow);
        } else {
            AffirmTracker.track(CHECKOUT_WEBVIEW_CLICK, INFO, trackInfo);
            CheckoutActivity.startActivity(activity, checkoutRequest, checkout, caas,
                    cardAuthWindow);
        }
    }

    /**
     * Start checkout flow/ vcn checkout flow. Don't forget to call onActivityResult
     * to get the processed result
     *
     * @param fragment       fragment {@link Fragment}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param caas           caas merchant-level attribute
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     * @param useVCN         Start VCN checkout or not
     */
    public static void startCheckout(@NonNull Fragment fragment, @NonNull Checkout checkout,
                                     @Nullable String caas, int cardAuthWindow, boolean useVCN) {
        AffirmUtils.requireNonNull(fragment, "fragment cannot be null");
        AffirmUtils.requireNonNull(checkout, "checkout cannot be null");
        JsonObject trackInfo = createTrackingCheckout(checkout, caas, cardAuthWindow, useVCN);
        if (useVCN) {
            AffirmTracker.track(VCN_CHECKOUT_CREATION_CLICK, INFO, trackInfo);
            VcnCheckoutActivity.startActivity(fragment, vcnCheckoutRequest, checkout, caas,
                    null, cardAuthWindow, false);
        } else {
            AffirmTracker.track(CHECKOUT_WEBVIEW_CLICK, INFO, trackInfo);
            CheckoutActivity.startActivity(fragment, checkoutRequest, checkout, caas,
                    cardAuthWindow);
        }
    }

    /**
     * Check if there is a cached card
     */
    public static boolean existCachedCard() {
        return AffirmPlugins.get().getCachedCardDetails() != null;
    }

    /**
     * Start new VCN checkout flow - Contains loan amount page & vcn display page
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     */
    public static void startNewVcnCheckoutFlow(@NonNull Activity activity,
                                               @NonNull Checkout checkout) {
        startNewVcnCheckoutFlow(activity, checkout, null);
    }

    /**
     * Start new VCN checkout flow - Contains loan amount page & vcn display page
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     */
    public static void startNewVcnCheckoutFlow(@NonNull Activity activity,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas) {
        startNewVcnCheckoutFlow(activity, checkout, caas, -1);
    }

    /**
     * Start new VCN checkout flow - Contains loan amount page & vcn display page
     *
     * @param fragment fragment {@link Fragment}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     */
    public static void startNewVcnCheckoutFlow(@NonNull Fragment fragment,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas) {
        startNewVcnCheckoutFlow(fragment, checkout, caas, -1);
    }

    /**
     * Start new VCN checkout flow - Contains loan amount page & vcn display page
     *
     * @param activity       activity {@link Activity}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param caas           caas merchant-level attribute
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     */
    public static void startNewVcnCheckoutFlow(@NonNull Activity activity,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas,
                                               int cardAuthWindow) {
        AffirmUtils.requireNonNull(activity);
        AffirmUtils.requireNonNull(checkout);
        startLoanAmount(activity, checkout, caas, cardAuthWindow);
    }

    /**
     * Start new VCN checkout flow - Contains loan amount page & vcn display page
     *
     * @param fragment       fragment {@link Fragment}
     * @param checkout       checkout object that contains address & shipping info & others...
     * @param caas           caas merchant-level attribute
     * @param cardAuthWindow the value is a positive integer, 0 being a valid value
     */
    public static void startNewVcnCheckoutFlow(@NonNull Fragment fragment,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas,
                                               int cardAuthWindow) {
        AffirmUtils.requireNonNull(fragment);
        AffirmUtils.requireNonNull(checkout);
        startLoanAmount(fragment, checkout, caas, cardAuthWindow);
    }

    /**
     * Start vcn display page from merchant
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     */
    public static void startVcnDisplay(@NonNull Activity activity,
                                       @NonNull Checkout checkout) {
        startVcnDisplay(activity, checkout, null);
    }

    /**
     * Start vcn display page from merchant
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     */
    public static void startVcnDisplay(@NonNull Activity activity,
                                       @NonNull Checkout checkout,
                                       @Nullable String caas) {
        CardDetailsInner cardDetailsInner = AffirmPlugins.get().getCachedCardDetails();
        if (cardDetailsInner == null) {
            throw new IllegalStateException("No cached checkout or checkout have expired");
        }
        VcnDisplayActivity.startActivity(activity, vcnCheckoutRequest, checkout, caas);
    }

    /**
     * Start vcn display page from merchant
     *
     * @param fragment fragment {@link Fragment}
     * @param checkout checkout object that contains address & shipping info & others...
     * @param caas     caas merchant-level attribute
     */
    public static void startVcnDisplay(@NonNull Fragment fragment,
                                       @NonNull Checkout checkout,
                                       @Nullable String caas) {
        CardDetailsInner cardDetailsInner = AffirmPlugins.get().getCachedCardDetails();
        if (cardDetailsInner == null) {
            throw new IllegalStateException("No cached checkout or checkout have expired");
        }
        VcnDisplayActivity.startActivity(fragment, vcnCheckoutRequest, checkout, caas);
    }

    /**
     * Start loan amount page
     *
     * @param activity activity {@link Activity}
     * @param checkout checkout object that contains address & shipping info & others...
     */
    protected static void startLoanAmount(@NonNull Activity activity, @NonNull Checkout checkout,
                                          @Nullable String caas, int cardAuthWindow) {
        LoanAmountActivity.startActivity(activity, vcnCheckoutRequest, checkout, caas,
                cardAuthWindow);
    }

    /**
     * Start loan amount page
     *
     * @param fragment fragment {@link Fragment}
     * @param checkout checkout object that contains address & shipping info & others...
     */
    protected static void startLoanAmount(@NonNull Fragment fragment, @NonNull Checkout checkout,
                                          @Nullable String caas, int cardAuthWindow) {
        LoanAmountActivity.startActivity(fragment, vcnCheckoutRequest, checkout, caas,
                cardAuthWindow);
    }

    /**
     * Start vcn checkout flow.
     */
    protected static void startVcnCheckout(@NonNull Activity activity, @NonNull Checkout checkout,
                                           @Nullable String caas, @Nullable Money money,
                                           boolean newFlow, int cardAuthWindow) {
        VcnCheckoutActivity.startActivity(activity, vcnCheckoutRequest, checkout, caas, money,
                cardAuthWindow, newFlow);
    }

    /**
     * Use a `AffirmFragment` to start checkout flow/ vcn checkout flow. And in you host activity,
     * you need implements CheckoutCallbacks / VcnCheckoutCallbacks
     *
     * @param activity        activity {@link AppCompatActivity}
     * @param containerViewId The specified view will contain the fragment
     * @param checkout        checkout object that contains address & shipping info & others...
     * @param caas            caas merchant-level attribute
     * @param cardAuthWindow  the value is a positive integer, 0 being a valid value
     * @param useVCN          Start VCN checkout or not
     * @return a `AffirmFragment`
     */
    public static AffirmFragment startCheckout(@NonNull AppCompatActivity activity,
                                               @IdRes int containerViewId,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas,
                                               int cardAuthWindow,
                                               boolean useVCN) {
        if (useVCN) {
            return VcnCheckoutFragment.newInstance(activity,
                    containerViewId, checkout, receiveReasonCodes, caas, null,
                    cardAuthWindow, false);
        } else {
            return CheckoutFragment.newInstance(activity,
                    containerViewId, checkout, caas, cardAuthWindow);
        }
    }

    /**
     * Use a `AffirmFragment` to start checkout flow/ vcn checkout flow. And in you host activity,
     * you need implements CheckoutCallbacks / VcnCheckoutCallbacks
     *
     * @param fragment        fragment {@link Fragment}
     * @param containerViewId The specified view will contain the fragment
     * @param checkout        checkout object that contains address & shipping info & others...
     * @param caas            caas merchant-level attribute
     * @param cardAuthWindow  the value is a positive integer, 0 being a valid value
     * @param useVCN          Start VCN checkout or not
     * @return a `AffirmFragment`
     */
    public static AffirmFragment startCheckout(@NonNull Fragment fragment,
                                               @IdRes int containerViewId,
                                               @NonNull Checkout checkout,
                                               @Nullable String caas,
                                               int cardAuthWindow,
                                               boolean useVCN) {
        if (useVCN) {
            return VcnCheckoutFragment.newInstance(fragment,
                    containerViewId, checkout, receiveReasonCodes, caas, null,
                    cardAuthWindow, false);
        } else {
            return CheckoutFragment.newInstance(fragment,
                    containerViewId, checkout, caas, cardAuthWindow);
        }
    }

    /**
     * Use a `AffirmFragment` to start checkout flow/ vcn checkout flow. And in you host activity,
     * you need implements CheckoutCallbacks / VcnCheckoutCallbacks
     *
     * @param activity        activity {@link AppCompatActivity}
     * @param containerViewId The specified view will contain the fragment
     * @param checkout        checkout object that contains address & shipping info & others...
     * @param caas            caas merchant-level attribute
     * @param money           momoney that user inputney
     * @param cardAuthWindow  the value is a positive integer, 0 being a valid value
     * @param newFlow         new flow
     * @param useVCN          Start VCN checkout or not
     * @return a `AffirmFragment`
     */
    protected static AffirmFragment startCheckout(@NonNull AppCompatActivity activity,
                                                  @IdRes int containerViewId,
                                                  @NonNull Checkout checkout,
                                                  @Nullable String caas,
                                                  @Nullable Money money,
                                                  int cardAuthWindow,
                                                  boolean newFlow,
                                                  boolean useVCN) {
        if (useVCN) {
            return VcnCheckoutFragment.newInstance(activity,
                    containerViewId, checkout, receiveReasonCodes, caas, money, cardAuthWindow,
                    newFlow);
        } else {
            return CheckoutFragment.newInstance(activity,
                    containerViewId, checkout, caas, cardAuthWindow);
        }
    }

    /**
     * Use a `AffirmFragment` to start checkout flow/ vcn checkout flow. And in you host activity,
     * you need implements CheckoutCallbacks / VcnCheckoutCallbacks
     *
     * @param fragment        fragment {@link Fragment}
     * @param containerViewId The specified view will contain the fragment
     * @param checkout        checkout object that contains address & shipping info & others...
     * @param caas            caas merchant-level attribute
     * @param money           money that user input
     * @param cardAuthWindow  the value is a positive integer, 0 being a valid value
     * @param newFlow         new flow
     * @param useVCN          Start VCN checkout or not
     * @return a `AffirmFragment`
     */
    protected static AffirmFragment startCheckout(@NonNull Fragment fragment,
                                                  @IdRes int containerViewId,
                                                  @NonNull Checkout checkout,
                                                  @Nullable String caas,
                                                  @Nullable Money money,
                                                  int cardAuthWindow,
                                                  boolean newFlow,
                                                  boolean useVCN) {
        if (useVCN) {
            return VcnCheckoutFragment.newInstance(fragment,
                    containerViewId, checkout, receiveReasonCodes, caas, money, cardAuthWindow,
                    newFlow);
        } else {
            return CheckoutFragment.newInstance(fragment,
                    containerViewId, checkout, caas, cardAuthWindow);
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
     * @param fragment fragment {@link Fragment}
     * @param modalId  the client's modal id
     */
    public static void showSiteModal(@NonNull Fragment fragment, @Nullable String modalId) {
        showSiteModal(fragment, modalId, null, null);
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
        AffirmUtils.requireNonNull(activity);
        ModalActivity.startActivity(activity, 0, BigDecimal.valueOf(0.0), SITE,
                modalId, pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Start site modal
     *
     * @param fragment fragment {@link Fragment}
     * @param modalId  the client's modal id
     * @param pageType need to use one of "banner, cart, category, homepage, landing,
     *                 payment, product, search"
     */
    public static void showSiteModal(@NonNull Fragment fragment, @Nullable String modalId,
                                     @Nullable PromoPageType pageType, @Nullable String promoId) {
        AffirmUtils.requireNonNull(fragment, "fragment cannot be null");
        ModalActivity.startActivity(fragment, 0, BigDecimal.valueOf(0.0), SITE, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Use a `AffirmFragment` to start site modal.
     *
     * @param activity        activity {@link AppCompatActivity}
     * @param containerViewId The specified view will contain the fragment
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param modalId         the client's modal id
     * @return a `ModalFragment`
     */
    public static AffirmFragment showSiteModal(@NonNull AppCompatActivity activity,
                                               @IdRes int containerViewId,
                                               @Nullable PromoPageType pageType,
                                               @Nullable String modalId) {
        return ModalFragment.newInstance(activity, containerViewId, BigDecimal.valueOf(0.0), SITE,
                modalId, pageType != null ? pageType.getType() : null, null);
    }

    /**
     * Use a `AffirmFragment` to start site modal.
     *
     * @param fragment        activity {@link Fragment}
     * @param containerViewId The specified view will contain the fragment
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param modalId         the client's modal id
     * @return a `ModalFragment`
     */
    public static AffirmFragment showSiteModal(@NonNull Fragment fragment,
                                               @IdRes int containerViewId,
                                               @Nullable PromoPageType pageType,
                                               @Nullable String modalId) {
        return ModalFragment.newInstance(fragment, containerViewId, BigDecimal.valueOf(0.0), SITE,
                modalId, pageType != null ? pageType.getType() : null, null);
    }


    /**
     * Start product modal
     *
     * @param activity activity {@link Activity}
     * @param amount   (BigDecimal) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     */
    public static void showProductModal(@NonNull Activity activity, BigDecimal amount,
                                        @Nullable String modalId) {
        showProductModal(activity, amount, modalId, null, null);
    }

    /**
     * Start product modal
     *
     * @param fragment fragment {@link Fragment}
     * @param amount   (BigDecimal) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     */
    public static void showProductModal(@NonNull Fragment fragment, BigDecimal amount,
                                        @Nullable String modalId) {
        showProductModal(fragment, amount, modalId, null, null);
    }

    /**
     * Start product modal
     *
     * @param activity activity {@link Activity}
     * @param amount   (BigDecimal) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal i
     * @param pageType need to use one of "banner, cart, category, homepage, landing,
     *                 payment, product, search"
     */
    public static void showProductModal(@NonNull Activity activity,
                                        BigDecimal amount,
                                        @Nullable String modalId,
                                        @Nullable PromoPageType pageType,
                                        @Nullable String promoId) {
        AffirmUtils.requireNonNull(activity);
        ModalActivity.startActivity(activity, 0, amount, PRODUCT, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Start product modal
     *
     * @param fragment fragment {@link Fragment}
     * @param amount   (BigDecimal) eg 112.02 as $112 and ¢2
     * @param modalId  the client's modal id
     * @param pageType need to use one of "banner, cart, category, homepage, landing,
     *                 payment, product, search"
     */
    public static void showProductModal(@NonNull Fragment fragment,
                                        BigDecimal amount,
                                        @Nullable String modalId,
                                        @Nullable PromoPageType pageType,
                                        @Nullable String promoId) {
        AffirmUtils.requireNonNull(fragment, "fragment cannot be null");
        ModalActivity.startActivity(fragment, 0, amount, PRODUCT, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Use a `AffirmFragment` to start product modal.
     *
     * @param activity        activity {@link AppCompatActivity}
     * @param containerViewId The specified view will contain the fragment
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param modalId         the client's modal id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param promoId         the client's promo id
     * @return a `ModalFragment`
     */
    public static AffirmFragment showProductModal(@NonNull AppCompatActivity activity,
                                                  @IdRes int containerViewId,
                                                  BigDecimal amount,
                                                  @Nullable String modalId,
                                                  @Nullable PromoPageType pageType,
                                                  @Nullable String promoId) {
        return ModalFragment.newInstance(activity, containerViewId, amount, PRODUCT, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Use a `AffirmFragment` to start product modal.
     *
     * @param fragment        activity {@link Fragment}
     * @param containerViewId The specified view will contain the fragment
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param modalId         the client's modal id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param promoId         the client's promo id
     * @return a `ModalFragment`
     */
    public static AffirmFragment showProductModal(@NonNull Fragment fragment,
                                                  @IdRes int containerViewId,
                                                  BigDecimal amount,
                                                  @Nullable String modalId,
                                                  @Nullable PromoPageType pageType,
                                                  @Nullable String promoId) {
        return ModalFragment.newInstance(fragment, containerViewId, amount, PRODUCT, modalId,
                pageType != null ? pageType.getType() : null, promoId);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
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
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           final BigDecimal amount,
                                           final boolean showCta,
                                           @Nullable final List<Item> items) {
        configureWithAmount(promotionButton, null, null, amount, showCta, items);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's modal id
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
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
     * @param promoId         the client's modal id
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final String promoId,
                                           final BigDecimal amount,
                                           final boolean showCta,
                                           @Nullable final List<Item> items) {
        configureWithAmount(promotionButton, promoId, null, amount, showCta, items);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
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
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final PromoPageType pageType,
                                           final BigDecimal amount,
                                           final boolean showCta,
                                           @Nullable final List<Item> items) {
        configureWithAmount(promotionButton, null, pageType, amount, showCta, items);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's modal id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final String promoId,
                                           @Nullable final PromoPageType pageType,
                                           final BigDecimal amount,
                                           final boolean showCta) {
        configureWithAmount(promotionButton, promoId, pageType, amount, showCta, null);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's modal id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (BigDecimal) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(@NonNull final AffirmPromotionButton promotionButton,
                                           @Nullable final String promoId,
                                           @Nullable final PromoPageType pageType,
                                           final BigDecimal amount,
                                           final boolean showCta,
                                           @Nullable final List<Item> items) {
        AffirmUtils.requireNonNull(promotionButton, "AffirmPromotionButton cannot be null");
        final View.OnClickListener onClickListener = promotionView -> {
            Activity activity = AffirmUtils.getActivityFromView(promotionView);
            if (activity == null || promotionButton.isEmpty()) {
                return;
            }
            boolean showPrequal = (boolean) promotionView.getTag();
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
        configureWithAmount(promotionButton, promoId, pageType, amount, showCta, items,
                onClickListener);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param activity        activity {@link AppCompatActivity}
     * @param containerViewId The specified view will contain the fragment
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's promo id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(
            @NonNull AppCompatActivity activity,
            @IdRes int containerViewId,
            @NonNull final AffirmPromotionButton promotionButton,
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final BigDecimal amount,
            final boolean showCta,
            @Nullable final List<Item> items) {

        final View.OnClickListener onClickListener = promotionView -> {
            if (promotionButton.isEmpty()) {
                return;
            }
            boolean showPrequal = (boolean) promotionView.getTag();
            String type = pageType != null ? pageType.getType() : null;
            if (showPrequal) {
                PrequalFragment.newInstance(activity, containerViewId, amount,
                        promoId, type);
            } else {
                ModalFragment.newInstance(activity, containerViewId, amount,
                        PRODUCT, null, type, promoId);
            }
        };
        configureWithAmount(promotionButton, promoId, pageType, amount, showCta, items,
                onClickListener);
    }

    /**
     * Write the as low as span (text and logo) on a AffirmPromoLabel
     *
     * @param fragment        activity {@link Fragment}
     * @param containerViewId The specified view will contain the fragment
     * @param promotionButton AffirmPromotionButton to show the promo message
     * @param promoId         the client's promo id
     * @param pageType        need to use one of "banner, cart, category, homepage, landing,
     *                        payment, product, search"
     * @param amount          (Float) eg 112.02 as $112 and ¢2
     * @param showCta         whether need to show cta
     * @param items           A list of item objects.
     */
    public static void configureWithAmount(
            @NonNull Fragment fragment,
            @IdRes int containerViewId,
            @NonNull final AffirmPromotionButton promotionButton,
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final BigDecimal amount,
            final boolean showCta,
            @Nullable final List<Item> items) {

        final View.OnClickListener onClickListener = promotionView -> {
            if (promotionButton.isEmpty()) {
                return;
            }
            boolean showPrequal = (boolean) promotionView.getTag();
            String type = pageType != null ? pageType.getType() : null;
            if (showPrequal) {
                PrequalFragment.newInstance(fragment, containerViewId, amount,
                        promoId, type);
            } else {
                ModalFragment.newInstance(fragment, containerViewId, amount,
                        PRODUCT, null, type, promoId);
            }
        };
        configureWithAmount(promotionButton, promoId, pageType, amount, showCta, items,
                onClickListener);
    }

    private static void configureWithAmount(
            @NonNull final AffirmPromotionButton promotionButton,
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final BigDecimal amount,
            final boolean showCta,
            @Nullable final List<Item> items,
            View.OnClickListener onClickListener) {
        AffirmUtils.requireNonNull(promotionButton,
                "AffirmPromotionButton cannot be null");
        final SpannablePromoCallback callback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promoMessage,
                                       @NonNull String promoDescription,
                                       boolean showPrequal) {
                promotionButton.setTag(showPrequal);
                promotionButton.setLabel(promoMessage, promoDescription);
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
                        items,
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
            @NonNull final PromotionCallbackV2 callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promoMessage,
                                       @NonNull String promoDescription,
                                       boolean showPrequal) {
                callback.onSuccess(
                        new Promotion(
                                AffirmUtils.createSpannableForText(
                                        promoMessage,
                                        textSize,
                                        requestData.getAffirmLogoType(),
                                        requestData.getAffirmColor(),
                                        context
                                ),
                                promoDescription,
                                showPrequal
                        )
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
     * Fetch promotional message, you can display it yourself
     *
     * @param requestData a class containing data about the request to make
     * @param textSize    the textSize for the span
     * @param context     the context being used
     * @param callback    a class that's called when the request completes
     */
    @Deprecated
    public static AffirmRequest fetchPromotion(
            @NonNull PromoRequestData requestData,
            float textSize,
            @NonNull Context context,
            @NonNull final PromotionCallback callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promoMessage,
                                       @NonNull String promoDescription,
                                       boolean showPrequal) {
                callback.onSuccess(
                        AffirmUtils.createSpannableForText(
                                promoMessage,
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
            @NonNull final HtmlPromotionCallbackV2 callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promoMessage,
                                       @NonNull String promoDescription,
                                       boolean showPrequal) {
                callback.onSuccess(new HtmlPromotion(promoMessage, promoDescription, showPrequal));
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                callback.onFailure(exception);
            }
        };
        return buildPromoRequest(requestData, promoCallback, true);
    }

    /**
     * Fetch promotional html message, you can display it yourself
     *
     * @param requestData a class containing data about the request to make
     * @param callback    a class that's called when the request completes
     */
    @Deprecated
    public static AffirmRequest fetchHtmlPromotion(
            @NonNull PromoRequestData requestData,
            @NonNull final HtmlPromotionCallback callback
    ) {
        SpannablePromoCallback promoCallback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull String promoMessage,
                                       @NonNull String promoDescription,
                                       boolean showPrequal) {
                callback.onSuccess(promoMessage, showPrequal);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
                callback.onFailure(exception);
            }
        };
        return buildPromoRequest(requestData, promoCallback, true);
    }

    private static PromoRequest buildPromoRequest(@NonNull PromoRequestData requestData,
                                                  SpannablePromoCallback promoCallback,
                                                  Boolean isHtmlStyle) {
        return new PromoRequest(
                requestData.getPromoId(),
                requestData.getPageType(),
                requestData.getAmount(),
                requestData.showCta(),
                requestData.getAffirmColor(),
                requestData.getAffirmLogoType(),
                isHtmlStyle,
                requestData.getItems(),
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
            PrequalActivity.startActivity(activity,
                    prequalRequest,
                    promoRequestModal.getAmount(),
                    promoRequestModal.getPromoId(),
                    type);
        } else {
            ModalActivity.startActivity(activity,
                    prequalRequest,
                    promoRequestModal.getAmount(),
                    PRODUCT,
                    null,
                    type,
                    promoRequestModal.getPromoId());
        }
    }

    /**
     * Handling events that click on the promotion message
     *
     * @param fragment          fragment {@link Fragment}
     * @param promoRequestModal a class contains the parameters required for the request
     * @param showPrequal       This value comes from the callback of the method `fetchPromotion`
     */
    public static void onPromotionClick(@NonNull Fragment fragment,
                                        @NonNull PromoRequestData promoRequestModal,
                                        boolean showPrequal) {
        PromoPageType pageType = promoRequestModal.getPageType();
        String type = pageType != null ? pageType.getType() : null;
        if (showPrequal) {
            PrequalActivity.startActivity(fragment,
                    prequalRequest,
                    promoRequestModal.getAmount(),
                    promoRequestModal.getPromoId(),
                    type);
        } else {
            ModalActivity.startActivity(fragment,
                    prequalRequest,
                    promoRequestModal.getAmount(),
                    PRODUCT,
                    null,
                    type,
                    promoRequestModal.getPromoId());
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
                case RESULT_CHECKOUT_CANCEL:
                    callbacks.onAffirmVcnCheckoutCancelledReason(VcnReason.builder()
                            .setReason("Checkout canceled")
                            .build());
                    break;
                default:
                    break;
            }

            return true;
        }

        return false;
    }
}

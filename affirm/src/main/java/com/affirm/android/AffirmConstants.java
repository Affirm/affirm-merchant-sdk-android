package com.affirm.android;

public final class AffirmConstants {

    private AffirmConstants() {
    }

    static final String SDK_NAME = "Affirm";

    // Default location is US
    private static Affirm.Location location = Affirm.Location.US;

    static void setLocation(Affirm.Location location) {
        AffirmConstants.location = location;
    }

    static String getSandboxUrl() {
        switch (location) {
            case CA:
                return CA_SANDBOX_URL;
            default:
                return SANDBOX_URL;
        }
    }

    static String getSandboxJsUrl() {
        switch (location) {
            case CA:
                return CA_SANDBOX_JS_URL;
            default:
                return SANDBOX_JS_URL;
        }
    }

    static String getTrackerUrl() {
        switch (location) {
            case CA:
                return CA_TRACKER_URL;
            default:
                return TRACKER_URL;
        }
    }

    static String getProductionUrl() {
        switch (location) {
            case CA:
                return CA_PRODUCTION_URL;
            default:
                return PRODUCTION_URL;
        }
    }

    static String getProductionJsUrl() {
        switch (location) {
            case CA:
                return CA_PRODUCTION_JS_URL;
            default:
                return PRODUCTION_JS_URL;
        }
    }

    static String getStagingPromoUrl() {
        switch (location) {
            case CA:
                return CA_SANDBOX_URL;
            default:
                return SANDBOX_URL;
        }
    }

    static String getProductionPromoUrl() {
        switch (location) {
            case CA:
                return CA_PRODUCTION_PROMO_URL;
            default:
                return PRODUCTION_PROMO_URL;
        }
    }

    static String getStagingInvalidCheckoutRedirectUrl() {
        switch (location) {
            case CA:
                return CA_SANDBOX_INVALID_CHECKOUT_REDIRECT_URL;
            default:
                return SANDBOX_INVALID_CHECKOUT_REDIRECT_URL;
        }
    }

    static String getProductionInvalidCheckoutRedirectUrl() {
        switch (location) {
            case CA:
                return CA_PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL;
            default:
                return PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL;
        }
    }

    // CA URL
    private static final String CA_SANDBOX_URL = "sandbox.affirm.ca";
    private static final String CA_SANDBOX_JS_URL = "cdn1-sandbox.affirm.ca";
    private static final String CA_SANDBOX_INVALID_CHECKOUT_REDIRECT_URL = "sandbox.affirm.ca/u/";
    private static final String CA_TRACKER_URL = "tracker.affirm.ca";
    private static final String CA_PRODUCTION_URL = "api.affirm.ca";
    private static final String CA_PRODUCTION_PROMO_URL = "www.affirm.ca";
    private static final String CA_PRODUCTION_JS_URL = "cdn1.affirm.ca";
    private static final String CA_PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL = "api.affirm.ca/u/";

    // US URL
    private static final String SANDBOX_URL = "sandbox.affirm.com";
    private static final String SANDBOX_JS_URL = "cdn1-sandbox.affirm.com";
    private static final String SANDBOX_INVALID_CHECKOUT_REDIRECT_URL = "sandbox.affirm.com/u/";
    private static final String TRACKER_URL = "tracker.affirm.com";
    private static final String PRODUCTION_URL = "api.affirm.com";
    private static final String PRODUCTION_PROMO_URL = "www.affirm.com";
    private static final String PRODUCTION_JS_URL = "cdn1.affirm.com";
    private static final String PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL = "api.affirm.com/u/";

    static final String CHECKOUT_PATH = "/api/v2/checkout/";
    static final String TRACKER_PATH = "/collect";
    static final String PROMO_PATH = "/api/promos/v2/%s?is_sdk=true&field=ala&amount=%d"
            + "&show_cta=%s";
    static final String PREQUAL_PATH = "/apps/prequal?public_api_key=%s&unit_price=%s"
            + "&promo_external_id=%s&isSDK=true&use_promo=true&referring_url=%s";
    static final String AFFIRM_CHECKOUT_CONFIRMATION_URL = "affirm://checkout/confirmed";
    static final String AFFIRM_CHECKOUT_CANCELLATION_URL = "affirm://checkout/cancelled";
    static final String REFERRING_URL = "https://androidsdk/";
    static final String JS_PATH = "/js/v2/affirm.js";

    static final String TAG_GET_NEW_PROMO = "GET_NEW_PROMO";
    static final String TAG_CHECKOUT = "CHECKOUT";
    static final String TAG_VCN_CHECKOUT = "VCN_CHECKOUT";
    static final String TAG_TRACKER = "TAG_TRACKER";

    static final String HTTPS_PROTOCOL = "https://";
    static final String HTTP_PROTOCOL = "http://";
    static final String HTTP = "http";
    static final String X_AFFIRM_REQUEST_ID = "X-Affirm-Request-Id";
    static final String CONTENT_TYPE = "application/json; charset=utf-8";
    static final String TEXT_HTML = "text/html";
    static final String UTF_8 = "utf-8";

    static final String AMOUNT = "AMOUNT";
    static final String PROMO_ID = "PROMO_ID";
    static final String PAGE_TYPE = "PAGE_TYPE";
    static final String MAP_EXTRA = "MAP_EXTRA";
    static final String TYPE_EXTRA = "TYPE_EXTRA";

    static final String API_KEY = "API_KEY";
    static final String JAVASCRIPT = "JAVASCRIPT";
    static final String CANCEL_URL = "CANCEL_URL";
    static final String MODAL_ID = "MODAL_ID";

    static final String LOGO_PLACEHOLDER = "{affirm_logo}";
    static final String PLACEHOLDER_START = "{{";
    static final String PLACEHOLDER_END = "}}";

    static final String CHECKOUT_TOKEN = "checkout_token";
    static final String CHECKOUT_ERROR = "checkout_error";
    static final String CHECKOUT_EXTRA = "checkout_extra";
    static final String CHECKOUT_RECEIVE_REASON_CODES = "checkout_receive_reason_codes";
    static final String CREDIT_DETAILS = "credit_details";
    static final String VCN_REASON = "vcn_reason";

    static final String PREQUAL_ERROR = "prequal_error";

    static final String URL = "URL";
    static final String URL2 = "URL2";
    static final String CONFIRM_CB_URL = "CONFIRM_CB_URL";
    static final String CANCELLED_CB_URL = "CANCELLED_CB_URL";

    static final String TRACK_ORDER_OBJECT = "TRACK_ORDER_OBJECT";
    static final String TRACK_PRODUCT_OBJECT = "TRACK_PRODUCT_OBJECT";

    static final String HTML_FRAGMENT = "HTML_FRAGMENT";
    static final String REMOTE_CSS_URL = "REMOTE_CSS_URL";
    static final String AFFIRM_FONT = "AFFIRM_FONT";

    static final String USER_CONFIRMATION_URL_ACTION_KEY = "user_confirmation_url_action";
    static final String USER_CONFIRMATION_URL_ACTION_VALUE = "GET";

    static final String PLATFORM_TYPE_KEY = "platform_type";
    static final String PLATFORM_TYPE_VALUE = "Affirm Android SDK";
    static final String PLATFORM_AFFIRM_KEY = "platform_affirm";
    static final String PLATFORM_AFFIRM_VALUE = BuildConfig.VERSION_NAME;

    static final String API_VERSION_KEY = "api_version";
    static final String API_VERSION_VALUE = "v2";

    static final String MERCHANT = "merchant";
    static final String METADATA = "metadata";
    static final String CHECKOUT = "checkout";

    static final String INVALID_CHECKOUT_MESSAGE = "Checkout status is in an invalid state.";
}

package com.affirm.android;

public final class AffirmConstants {

    private AffirmConstants() {
    }

    static final String SDK_NAME = "Affirm";

    static final String COUNTY_CODE_CAN = "CAN";

    static final String COUNTY_CODE_USA = "USA";
    static final String LOCALE_USA = "en_US";

    // checkout url
    static final String SANDBOX_CHECKOUT_URL = "api.global-sandbox.affirm.com";
    static final String PRODUCTION_CHECKOUT_URL = "api.global.affirm.com";

    // js url
    static final String SANDBOX_JS_URL = "cdn1-sandbox.affirm.com";
    static final String PRODUCTION_JS_URL = "cdn1.affirm.com";

    // invalid checkout redirect url
    static final String SANDBOX_INVALID_CHECKOUT_REDIRECT_URL = "sandbox.affirm.com/u/";
    static final String PRODUCTION_INVALID_CHECKOUT_REDIRECT_URL = "api.affirm.com/u/";

    // promo url (Default)
    static final String SANDBOX_PROMO_URL = "sandbox.affirm.com";
    static final String PRODUCTION_PROMO_URL = "www.affirm.com";

    // promo url (CA)
    static final String SANDBOX_PROMO_CA_URL = "sandbox.affirm.ca";
    static final String PRODUCTION_PROMO_CA_URL = "www.affirm.ca";

    // tracker url
    static final String TRACKER_URL = "tracker.affirm.com";

    static final String CHECKOUT_PATH = "/api/v2/checkout/";
    static final String TRACKER_PATH = "/collect";
    static final String PREQUAL_PATH = "/apps/prequal";
    static final String PREQUAL_PUBLIC_API_KEY = "public_api_key";
    static final String PREQUAL_UNIT_PRICE = "unit_price";
    static final String PREQUAL_USE_PROMO = "use_promo";
    static final String PREQUAL_REFERRING_URL = "referring_url";
    static final String PREQUAL_PROMO_EXTERNAL_ID = "promo_external_id";
    static final String PREQUAL_PAGE_TYPE = "page_type";
    static final String PREQUAL_LOCALE = "locale";

    static final String AFFIRM_CHECKOUT_CONFIRMATION_URL = "affirm://checkout/confirmed";
    static final String AFFIRM_CHECKOUT_CANCELLATION_URL = "affirm://checkout/cancelled";
    static final String REFERRING_URL = "https://androidsdk/";
    static final String JS_PATH = "/js/v2/affirm.js";

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
    static final String ITEMS = "ITEMS";
    static final String MAP_EXTRA = "MAP_EXTRA";
    static final String TYPE_EXTRA = "TYPE_EXTRA";

    static final String API_KEY = "API_KEY";
    static final String JAVASCRIPT = "JAVASCRIPT";
    static final String LOCALE = "LOCALE";
    static final String COUNTRY_CODE = "COUNTRY_CODE";
    static final String CANCEL_URL = "CANCEL_URL";
    static final String MODAL_ID = "MODAL_ID";

    static final String LOGO_PLACEHOLDER = "{affirm_logo}";
    static final String PLACEHOLDER_START = "{{";
    static final String PLACEHOLDER_END = "}}";

    static final String CHECKOUT_TOKEN = "checkout_token";
    static final String CHECKOUT_ERROR = "checkout_error";
    static final String CHECKOUT_EXTRA = "checkout_extra";
    static final String CHECKOUT_RECEIVE_REASON_CODES = "checkout_receive_reason_codes";
    static final String CHECKOUT_CAAS_EXTRA = "checkout_caas_extra";
    static final String CHECKOUT_MONEY = "checkout_money";
    static final String CHECKOUT_CARD_AUTH_WINDOW = "checkout_card_auth_window";
    static final String CHECKOUT_META = "meta";
    static final String CHECKOUT_META_LOCALE = "locale";
    static final String CREDIT_DETAILS = "credit_details";
    static final String VCN_REASON = "vcn_reason";
    static final String NEW_FLOW = "new_flow";

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
    static final String TOTAL = "total";

    static final String INVALID_CHECKOUT_MESSAGE = "Checkout status is in an invalid state.";
    static final String AFFIRM_NOT_INITIALIZED_MESSAGE = "Affirm has not been initialized";

    static final String PROMO_PATH = "/api/promos/v2/%s";
    static final String PROMO_IS_SDK = "is_sdk";
    static final String PROMO_FIELD = "field";
    static final String PROMO_FIELD_VALUE = "ala";
    static final String PROMO_AMOUNT = "amount";
    static final String PROMO_SHOW_CTA = "show_cta";
    static final String PROMO_EXTERNAL_ID = "promo_external_id";
    static final String PROMO_PAGE_TYPE = "page_type";
    static final String PROMO_LOGO_COLOR = "logo_color";
    static final String PROMO_LOGO_TYPE = "logo_type";
    static final String PROMO_ITEMS = "items";
    static final String PROMO_LOCALE = "locale";

    static final String CHECKOUT_HEADER_AFFIRM_LOCALE = "affirm-locale";
    static final String CHECKOUT_HEADER_COUNTRY_CODE = "country-code";


}

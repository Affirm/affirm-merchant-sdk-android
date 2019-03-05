package com.affirm.android;

class AffirmTracker {


    public enum TrackingEvent {
        // @formatter:off
        CHECKOUT_CREATION_FAIL("Checkout creation failed"),
        CHECKOUT_CREATION_SUCCESS("Checkout creation success"),
        CHECKOUT_WEBVIEW_SUCCESS("Checkout webView success"),
        CHECKOUT_WEBVIEW_FAIL("Checkout WebView failed"),
        VCN_CHECKOUT_CREATION_FAIL("Vcn Checkout creation failed"),
        VCN_CHECKOUT_CREATION_SUCCESS("Vcn Checkout creation success"),
        VCN_CHECKOUT_WEBVIEW_SUCCESS("Vcn Checkout webView success"),
        VCN_CHECKOUT_WEBVIEW_FAIL("Vcn Checkout webView failed"),
        PRODUCT_WEBVIEW_FAIL("Product webView failed"),
        SITE_WEBVIEW_FAIL("Site webView failed"),
        NETWORK_ERROR("network error");
        // @formatter:on

        private final String name;

        TrackingEvent(String name) {
            this.name = name;
        }
    }

    enum TrackingLevel {
        INFO("info"), WARNING("warning"), ERROR("error");

        private final String level;

        TrackingLevel(String level) {
            this.level = level;
        }

        protected String getLevel() {
            return this.level;
        }
    }
}

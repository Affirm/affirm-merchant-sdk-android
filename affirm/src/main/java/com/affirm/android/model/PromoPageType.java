package com.affirm.android.model;

public enum PromoPageType {
    BANNER("banner"),
    CART("cart"),
    CATEGORY("category"),
    HOMEPAGE("homepage"),
    LANDING("landing"),
    PAYMENT("payment"),
    PRODUCT("product"),
    SEARCH("search");

    private final String type;

    public String getType() {
        return type;
    }

    PromoPageType(String type) {
        this.type = type;
    }
}
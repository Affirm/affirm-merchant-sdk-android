package com.affirm.android.model;

public enum Currency {

    CAD("CAD"), USD("USD");

    private final String value;

    Currency(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}

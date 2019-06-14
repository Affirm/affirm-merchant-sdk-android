package com.affirm.android;

import androidx.annotation.ColorRes;

public enum AffirmColor {
    AFFIRM_COLOR_TYPE_BLUE(0),
    AFFIRM_COLOR_TYPE_BLACK(1),
    AFFIRM_COLOR_TYPE_WHITE(2);

    private static final String BLUE = "blue";
    private static final String BLACK = "black";
    private static final String WHITE = "white";

    private int ordinal;

    AffirmColor(int ordinal) {
        this.ordinal = ordinal;
    }

    protected static AffirmColor getAffirmColor(int ordinal) {
        AffirmColor[] types = values();
        for (AffirmColor type : types) {
            if (type.ordinal == ordinal) {
                return type;
            }
        }
        return AFFIRM_COLOR_TYPE_WHITE;
    }

    public int getOrdinal() {
        return ordinal;
    }

    @ColorRes
    protected int getColorRes() {
        switch (this) {
            case AFFIRM_COLOR_TYPE_BLACK:
                return R.color.affirm_black;
            case AFFIRM_COLOR_TYPE_BLUE:
                return R.color.affirm_blue;
            default:
                return R.color.affirm_white;
        }
    }

    protected String getColor() {
        switch (this) {
            case AFFIRM_COLOR_TYPE_BLUE:
                return BLUE;
            case AFFIRM_COLOR_TYPE_BLACK:
                return BLACK;
            case AFFIRM_COLOR_TYPE_WHITE:
                return WHITE;
            default:
                return BLUE;
        }
    }
}

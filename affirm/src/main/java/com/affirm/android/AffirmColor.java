package com.affirm.android;

import androidx.annotation.ColorRes;

public enum AffirmColor {
    AFFIRM_COLOR_TYPE_BLUE(0),
    AFFIRM_COLOR_TYPE_BLACK(1),
    AFFIRM_COLOR_TYPE_WHITE(2);

    private int mOrdinal;

    AffirmColor(int ordinal) {
        mOrdinal = ordinal;
    }

    protected static AffirmColor getAffirmColor(int ordinal) {
        AffirmColor[] types = values();
        for (AffirmColor type : types) {
            if (type.mOrdinal == ordinal) {
                return type;
            }
        }
        return AFFIRM_COLOR_TYPE_WHITE;
    }

    public int getOrdinal() {
        return mOrdinal;
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
}

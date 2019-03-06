package com.affirm.android;

import androidx.annotation.ColorRes;

enum AffirmColor {
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

    protected @ColorRes
    int getColorRes() {
        switch (this) {
            case AFFIRM_COLOR_TYPE_BLACK:
                return R.color.black100;
            case AFFIRM_COLOR_TYPE_BLUE:
                return R.color.blue;
            default:
                return R.color.white;
        }
    }
}

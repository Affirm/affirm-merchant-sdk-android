package com.affirm.android;

import androidx.annotation.DrawableRes;

enum AffirmLogoType {

    AFFIRM_DISPLAY_TYPE_LOGO(0),
    AFFIRM_DISPLAY_TYPE_TEXT(1),
    AFFIRM_DISPLAY_TYPE_SYMBOL(2),
    AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW(3);

    private int mOrdinal;

    AffirmLogoType(int ordinal) {
        mOrdinal = ordinal;
    }

    protected static AffirmLogoType getAffirmLogoType(int ordinal) {
        final AffirmLogoType[] types = values();
        for (AffirmLogoType type : types) {
            if (type.mOrdinal == ordinal) {
                return type;
            }
        }
        return AFFIRM_DISPLAY_TYPE_TEXT;
    }

    int getOrdinal() {
        return mOrdinal;
    }

    protected @DrawableRes
    int getDrawableRes() {
        switch (this) {
            case AFFIRM_DISPLAY_TYPE_LOGO:
                return R.drawable.black_logo_transparent_bg;
            case AFFIRM_DISPLAY_TYPE_SYMBOL:
                return R.drawable.black_solid_circle_transparent_bg;
            case AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW:
                return R.drawable.black_hollow_circle_transparent_bg;
            default:
                return R.drawable.black_logo_transparent_bg;
        }
    }
}

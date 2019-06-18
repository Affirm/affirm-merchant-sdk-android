package com.affirm.android;

import androidx.annotation.DrawableRes;

public enum AffirmLogoType {

    AFFIRM_DISPLAY_TYPE_LOGO(0),
    AFFIRM_DISPLAY_TYPE_TEXT(1),
    AFFIRM_DISPLAY_TYPE_SYMBOL(2),
    AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW(3);

    private static final String LOGO = "logo";
    private static final String TEXT = "text";
    private static final String SYMBOL = "symbol";

    private int ordinal;

    AffirmLogoType(int ordinal) {
        this.ordinal = ordinal;
    }

    protected static AffirmLogoType getAffirmLogoType(int ordinal) {
        final AffirmLogoType[] types = values();
        for (AffirmLogoType type : types) {
            if (type.ordinal == ordinal) {
                return type;
            }
        }
        return AFFIRM_DISPLAY_TYPE_TEXT;
    }

    int getOrdinal() {
        return ordinal;
    }

    protected @DrawableRes
    int getDrawableRes() {
        switch (this) {
            case AFFIRM_DISPLAY_TYPE_LOGO:
                return R.drawable.affirm_black_logo_transparent_bg;
            case AFFIRM_DISPLAY_TYPE_SYMBOL:
                return R.drawable.affirm_black_solid_circle_transparent_bg;
            case AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW:
                return R.drawable.affirm_black_hollow_circle_transparent_bg;
            default:
                return R.drawable.affirm_black_logo_transparent_bg;
        }
    }

    protected String getType() {
        switch (this) {
            case AFFIRM_DISPLAY_TYPE_LOGO:
                return LOGO;
            case AFFIRM_DISPLAY_TYPE_TEXT:
                return TEXT;
            case AFFIRM_DISPLAY_TYPE_SYMBOL:
                return SYMBOL;
            case AFFIRM_DISPLAY_TYPE_SYMBOL_HOLLOW:
                return SYMBOL;
            default:
                return LOGO;
        }
    }
}

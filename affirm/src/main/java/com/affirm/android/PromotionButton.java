package com.affirm.android;

import android.content.Context;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

class PromotionButton extends AppCompatButton {

    private AffirmLogoType affirmLogoType;
    private AffirmColor affirmColor;

    public void setAffirmLogoType(AffirmLogoType affirmLogoType) {
        this.affirmLogoType = affirmLogoType;
    }

    public void setAffirmColor(AffirmColor affirmColor) {
        this.affirmColor = affirmColor;
    }

    public void setAffirmTextSize(float affirmTextSize) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, affirmTextSize);
    }

    public void setAffirmTextColor(int colorRes) {
        setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }

    public PromotionButton(@NonNull Context context) {
        this(context, null);
    }

    public PromotionButton(@NonNull Context context,
                           @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PromotionButton(@NonNull Context context,
                           @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    SpannableString updateSpan(@NonNull String template) {
        float textSize = getTextSize();
        return AffirmUtils.createSpannableForText(template, textSize,
                affirmLogoType, affirmColor, getContext());
    }
}

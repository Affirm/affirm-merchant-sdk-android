package com.affirm.android;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import java.util.Locale;

class PromotionButton extends AppCompatButton {

    private Paint paint;
    private AffirmLogoType affirmLogoType;
    private AffirmColor affirmColor;

    public PromotionButton(@NonNull Context context) {
        super(context);
        init();
    }

    public PromotionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PromotionButton(@NonNull Context context,
                           @Nullable AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAffirmLogoType(AffirmLogoType affirmLogoType) {
        this.affirmLogoType = affirmLogoType;
    }

    public void setAffirmColor(AffirmColor affirmColor) {
        this.affirmColor = affirmColor;
    }

    public void setAffirmTextSize(float affirmTextSize) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, affirmTextSize);
    }

    SpannableString updateSpan(@NonNull String template) {
        float textSize = getTextSize();

        paint.setTextSize(textSize);
        paint.setTypeface(getTypeface());
        paint.getTextBounds(
                template.toUpperCase(Locale.getDefault()),
                0,
                template.length(),
                new Rect()
        );

        return AffirmUtils.createSpannableForText(
                template,
                textSize,
                affirmLogoType,
                affirmColor,
                getContext());
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }
}

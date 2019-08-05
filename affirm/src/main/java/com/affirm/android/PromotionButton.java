package com.affirm.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import static com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT;

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
        return spannableFromEditText(template, textSize);
    }

    private SpannableString spannableFromEditText(@NonNull String template, float textSize) {
        Resources resources = getResources();

        Drawable logoDrawable = null;
        if (affirmLogoType != AFFIRM_DISPLAY_TYPE_TEXT) {
            logoDrawable = resources.getDrawable(affirmLogoType.getDrawableRes());
        }

        final int color = resources.getColor(affirmColor.getColorRes());

        return getSpannable(template, textSize, logoDrawable, color);
    }

    private SpannableString getSpannable(@NonNull String template,
                                         float textSize,
                                         @Nullable Drawable logoDrawable,
                                         int color) {
        SpannableString spannableString;

        int index = template.indexOf(LOGO_PLACEHOLDER);
        if (logoDrawable != null && index != -1) {
            spannableString = new SpannableString(template);
            ImageSpan imageSpan = getLogoSpan(textSize, logoDrawable, color);
            spannableString.setSpan(imageSpan, index, index + LOGO_PLACEHOLDER.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } else {
            String onlyText = template.replace(LOGO_PLACEHOLDER, "Affirm");
            spannableString = new SpannableString(onlyText);
        }

        return spannableString;
    }

    private ImageSpan getLogoSpan(float textSize, @NonNull Drawable logoDrawable, int color) {

        float logoHeight = textSize * 1.f;
        float ratio = (float) logoDrawable.getIntrinsicWidth() / logoDrawable.getIntrinsicHeight();

        logoDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

        logoDrawable.setBounds(0, 0,
                Math.round(logoHeight * ratio), Math.round(logoHeight));
        return new ImageSpan(logoDrawable, ImageSpan.ALIGN_BASELINE);
    }

}

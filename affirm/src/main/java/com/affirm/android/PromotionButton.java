package com.affirm.android;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import static com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT;

class PromotionButton extends AppCompatButton {

    private Paint paint;
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

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
    }

    SpannableString updateSpan(@NonNull String template) {
        float textSize = getTextSize();
        Typeface typeface = getTypeface();
        return spannableFromEditText(template, textSize, typeface);
    }

    private SpannableString spannableFromEditText(@NonNull String template, float textSize,
                                                  @NonNull Typeface typeface) {
        Resources resources = getResources();

        Drawable logoDrawable = null;
        if (affirmLogoType != AFFIRM_DISPLAY_TYPE_TEXT) {
            logoDrawable = resources.getDrawable(affirmLogoType.getDrawableRes());
        }

        final int color = resources.getColor(affirmColor.getColorRes());

        return getSpannable(template, textSize, logoDrawable, typeface, color);
    }

    private SpannableString getSpannable(@NonNull String template,
                                         float textSize,
                                         @Nullable Drawable logoDrawable,
                                         @NonNull Typeface typeface, int color) {

        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        Rect result = new Rect();
        paint.getTextBounds(template.toUpperCase(Locale.getDefault()),
                0, template.length(), result);

        SpannableString spannableString;

        int index = template.indexOf(LOGO_PLACEHOLDER);
        if (logoDrawable != null && index != -1) {
            spannableString = new SpannableString(template);
            ImageSpan imageSpan = getLogoSpan(textSize, logoDrawable, color);
            spannableString.setSpan(imageSpan, index, index + LOGO_PLACEHOLDER.length(),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } else {
            String onlyText = template.replace(LOGO_PLACEHOLDER, "");
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

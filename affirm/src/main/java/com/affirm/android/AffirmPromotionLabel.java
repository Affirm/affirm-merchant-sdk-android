package com.affirm.android;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_WHITE;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT;
import static com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER;

public class AffirmPromotionLabel extends AppCompatTextView {

    private Paint mPaint;
    private AffirmLogoType mAffirmLogoType;
    private AffirmColor mAffirmColor;

    public AffirmPromotionLabel(@NonNull Context context) {
        this(context, null);
    }

    public AffirmPromotionLabel(@NonNull Context context,
                                @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AffirmPromotionLabel(@NonNull Context context,
                                @Nullable AttributeSet attrs,
                                int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.AffirmPromotionLabel);

        int affirmLogoTypeOrdinal =
                typedArray.getInt(R.styleable.AffirmPromotionLabel_affirmLogoType,
                        AFFIRM_DISPLAY_TYPE_TEXT.getOrdinal());

        int affirmColorOrdinal =
                typedArray.getInt(R.styleable.AffirmPromotionLabel_affirmColor,
                        AFFIRM_COLOR_TYPE_WHITE.getOrdinal());

        mAffirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        mAffirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
    }

    protected void setLabel(@NonNull String text) {
        setText(updateSpan(text));
    }

    public void setAffirmLogoType(AffirmLogoType affirmLogoType) {
        mAffirmLogoType = affirmLogoType;
    }

    public void setAffirmColor(@NonNull AffirmColor affirmColor) {
        mAffirmColor = affirmColor;
    }

    private SpannableString updateSpan(@NonNull String template) {
        float textSize = getTextSize();
        Typeface typeface = getTypeface();
        return spannableFromEditText(template, textSize, typeface);
    }

    private SpannableString spannableFromEditText(@NonNull String template, float textSize,
                                                  @NonNull Typeface typeface) {
        Resources resources = getResources();

        Drawable logoDrawable = null;
        if (mAffirmLogoType != AFFIRM_DISPLAY_TYPE_TEXT) {
            logoDrawable = resources.getDrawable(mAffirmLogoType.getDrawableRes());
        }

        final int color = resources.getColor(mAffirmColor.getColorRes());

        return getSpannable(template, textSize, logoDrawable, typeface, color);
    }

    private SpannableString getSpannable(@NonNull String template,
                                         float textSize,
                                         @Nullable Drawable logoDrawable,
                                         @NonNull Typeface typeface, int color) {

        mPaint.setTextSize(textSize);
        mPaint.setTypeface(typeface);
        Rect result = new Rect();
        mPaint.getTextBounds(template.toUpperCase(Locale.getDefault()),
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

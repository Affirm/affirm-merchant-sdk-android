package com.affirm.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO;

public class AffirmPromotionButton extends FrameLayout {

    private boolean htmlStyling;
    private String message;

    private PromotionButton promotionButton;
    private PromotionWebView promotionWebView;

    private AffirmLogoType affirmLogoType;
    private AffirmColor affirmColor;

    private String remoteCssUrl;
    private String typefaceDeclaration;

    public AffirmPromotionButton(@NonNull Context context) {
        this(context, null);
    }

    public AffirmPromotionButton(@NonNull Context context,
                                 @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AffirmPromotionButton(@NonNull Context context,
                                 @Nullable AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.AffirmPromotionButton);

        int affirmLogoTypeOrdinal =
                typedArray.getInt(R.styleable.AffirmPromotionButton_affirmLogoType,
                        AFFIRM_DISPLAY_TYPE_LOGO.getOrdinal());

        int affirmColorOrdinal =
                typedArray.getInt(R.styleable.AffirmPromotionButton_affirmColor,
                        AFFIRM_COLOR_TYPE_BLUE.getOrdinal());

        float affirmTextSize =
                typedArray.getDimensionPixelSize(R.styleable.AffirmPromotionButton_affirmTextSize,
                        getResources().getDimensionPixelSize(R.dimen.affirm_promotion_size));

        int affirmTextColor =
                typedArray.getResourceId(R.styleable.AffirmPromotionButton_affirmTextColor,
                        android.R.color.black);

        int affirmTextFont =
                typedArray.getResourceId(R.styleable.AffirmPromotionButton_affirmTextFont,
                        0);

        htmlStyling = typedArray.getBoolean(R.styleable.AffirmPromotionButton_htmlStyling,
                false);

        affirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        affirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();

        promotionButton = new PromotionButton(context);
        promotionButton.setAffirmTextSize(affirmTextSize);
        promotionButton.setAffirmTextColor(affirmTextColor);
        promotionButton.setTypeface(affirmTextFont > 0
                ? ResourcesCompat.getFont(getContext(), affirmTextFont) : Typeface.DEFAULT);
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);

        promotionWebView = new PromotionWebView(context);
        promotionWebView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    protected void setLabel(@NonNull String text) {
        this.message = text;
        removeAllViews();
        if (htmlStyling) {
            addView(promotionWebView);
            promotionWebView.loadWebData(text, remoteCssUrl, typefaceDeclaration);
        } else {
            addView(promotionButton);
            promotionButton.setText(promotionButton.updateSpan(text));
        }
    }

    public boolean isHtmlStyle() {
        return htmlStyling;
    }

    @Deprecated
    public void setAffirmLogoType(@NonNull AffirmLogoType affirmLogoType) {
        this.affirmLogoType = affirmLogoType;
        promotionButton.setAffirmLogoType(affirmLogoType);
    }

    @Deprecated
    public void setAffirmColor(@NonNull AffirmColor affirmColor) {
        this.affirmColor = affirmColor;
        promotionButton.setAffirmColor(affirmColor);
    }

    public void configWithHtmlStyling() {
        this.configWithHtmlStyling(null);
    }

    public void configWithHtmlStyling(@Nullable String remoteCssUrl) {
        this.configWithHtmlStyling(remoteCssUrl, null);
    }

    /**
     * When you want to display the promo message in html style, if you want to custom the style
     * of promo message, should pass the local or remote url and the file of typeface declaration
     *
     * @param remoteCssUrl        Set if you want to use custom css. If not, just pass null
     * @param typefaceDeclaration To embed your selected fonts into a webpage, you should pass
     *                            the typeface declaration.
     */
    public void configWithHtmlStyling(@Nullable String remoteCssUrl,
                                      @Nullable String typefaceDeclaration) {
        this.htmlStyling = true;
        this.remoteCssUrl = remoteCssUrl;
        this.typefaceDeclaration = typefaceDeclaration;
    }

    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType) {
        this.configWithLocalStyling(affirmColor, affirmLogoType, null);
    }

    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType,
                                       @Nullable Typeface typeface) {
        this.configWithLocalStyling(affirmColor, affirmLogoType, typeface,
                android.R.color.black, R.dimen.affirm_progressbar_dimen);
    }

    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType,
                                       @Nullable Typeface typeface,
                                       int affirmTextColor) {
        this.configWithLocalStyling(affirmColor, affirmLogoType, typeface,
                affirmTextColor, R.dimen.affirm_progressbar_dimen);
    }

    /**
     * When you want to display the promo message with a button, you can custom with the
     * AffirmColor, AffirmLogoType & Typeface
     *
     * @param affirmColor     Should be a kind of AffirmColor
     * @param affirmLogoType  Should be a kind of AffirmLogoType
     * @param typeface        The typeface you want to use
     * @param affirmTextColor The textColor of promo message
     * @param affirmTextSize  The textSize of promo message
     */
    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType,
                                       @Nullable Typeface typeface,
                                       int affirmTextColor,
                                       int affirmTextSize) {
        this.htmlStyling = false;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
        promotionButton.setTypeface(typeface);
        promotionButton.setAffirmTextColor(affirmTextColor);
        promotionButton.setAffirmTextSize(getResources().getDimensionPixelSize(affirmTextSize));
    }

    protected AffirmColor getAffirmColor() {
        return affirmColor;
    }

    protected AffirmLogoType getAffirmLogoType() {
        return affirmLogoType;
    }

    boolean isEmpty() {
        return TextUtils.isEmpty(message);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);

        if (htmlStyling) {
            promotionWebView.setWebViewClickListener(l);
        }
    }
}

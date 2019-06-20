package com.affirm.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

        htmlStyling = typedArray.getBoolean(R.styleable.AffirmPromotionButton_htmlStyling,
                false);

        affirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        affirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();

        promotionButton = new PromotionButton(context);
        promotionButton.setAffirmTextSize(affirmTextSize);
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
            promotionWebView.loadData(text, remoteCssUrl);
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

    public void configWithHtmlStyling(boolean htmlStyling) {
        this.configWithHtmlStyling(htmlStyling, null);
    }

    /**
     * When you want to display the promo message in html style, your should set htmlStyling to true
     * @param htmlStyling Set to true if you want to use html style
     * @param remoteCssUrl Set if you want to use custom css. If not, just pass null
     */
    public void configWithHtmlStyling(boolean htmlStyling, @Nullable String remoteCssUrl) {
        this.htmlStyling = htmlStyling;
        this.remoteCssUrl = remoteCssUrl;
    }

    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType) {
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
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

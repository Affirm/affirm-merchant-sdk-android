package com.affirm.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_WHITE;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_TEXT;

public class AffirmPromotionButton extends FrameLayout {

    private boolean htmlStyling;
    private String message;

    private PromotionButton promotionButton;
    private PromotionWebView promotionWebView;

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
                        AFFIRM_DISPLAY_TYPE_TEXT.getOrdinal());

        int affirmColorOrdinal =
                typedArray.getInt(R.styleable.AffirmPromotionButton_affirmColor,
                        AFFIRM_COLOR_TYPE_WHITE.getOrdinal());

        float affirmTextSize =
                typedArray.getDimensionPixelSize(R.styleable.AffirmPromotionButton_affirmTextSize,
                        getResources().getDimensionPixelSize(R.dimen.affirm_promotion_size));

        htmlStyling = typedArray.getBoolean(R.styleable.AffirmPromotionButton_htmlStyling,
                false);

        AffirmLogoType affirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        AffirmColor affirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();

        promotionButton = new PromotionButton(context);
        promotionButton.setAffirmTextSize(affirmTextSize);
        promotionButton.setVisibility(View.GONE);
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
        addView(promotionButton);

        promotionWebView = new PromotionWebView(context);
        promotionWebView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        promotionWebView.setVisibility(View.GONE);
        addView(promotionWebView);
    }

    protected void setLabel(@NonNull String text) {
        this.message = text;
        if (htmlStyling) {
            promotionButton.setVisibility(GONE);
            promotionWebView.setVisibility(VISIBLE);
            promotionWebView.loadData(text);
        } else {
            promotionButton.setVisibility(VISIBLE);
            promotionWebView.setVisibility(GONE);
            promotionButton.setText(promotionButton.updateSpan(text));
        }
    }

    public boolean isHtmlStyle() {
        return htmlStyling;
    }

    @Deprecated
    public void setAffirmLogoType(@NonNull AffirmLogoType affirmLogoType) {
        promotionButton.setAffirmLogoType(affirmLogoType);
    }

    @Deprecated
    public void setAffirmColor(@NonNull AffirmColor affirmColor) {
        promotionButton.setAffirmColor(affirmColor);
    }

    public void configWithHtmlStyling(boolean htmlStyling) {
        this.htmlStyling = htmlStyling;
    }

    public void configWithLocalStyling(@NonNull AffirmColor affirmColor,
                                       @NonNull AffirmLogoType affirmLogoType) {
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
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

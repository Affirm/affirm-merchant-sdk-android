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
    private boolean showPrequal;

    private PromotionButton promotionButton;
    private PromotionWebView promotionWebView;

    private AffirmLogoType affirmLogoType;
    private AffirmColor affirmColor;

    private String remoteCssUrl;
    private String typefaceDeclaration;

    private float affirmTextSize;
    private int affirmTextColor;
    private int affirmTextFont;

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

        affirmTextSize =
                typedArray.getDimensionPixelSize(R.styleable.AffirmPromotionButton_affirmTextSize,
                        getResources().getDimensionPixelSize(R.dimen.affirm_promotion_size));

        affirmTextColor =
                typedArray.getResourceId(R.styleable.AffirmPromotionButton_affirmTextColor,
                        android.R.color.black);

        affirmTextFont =
                typedArray.getResourceId(R.styleable.AffirmPromotionButton_affirmTextFont,
                        0);

        htmlStyling = typedArray.getBoolean(R.styleable.AffirmPromotionButton_htmlStyling,
                false);

        affirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        affirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();
    }

    protected void setLabel(@NonNull String promoMessage, @NonNull String promoDescription) {
        if (htmlStyling && promotionWebView == null) {
            // Released by destroy(); configWithHtmlStyling creates a new one.
            return;
        }
        if (promoMessage.equals(message)) {
            // Already shown, e.g. from the cache. Reloading would make it flash.
            return;
        }
        this.message = promoMessage;
        removeAllViews();
        if (htmlStyling) {
            addView(promotionWebView);
            promotionWebView.loadWebData(promoMessage, remoteCssUrl, typefaceDeclaration);
            promotionWebView.setContentDescription(promoDescription);
        } else {
            buildPromotionButtonIfNeeded();
            addView(promotionButton);
            promotionButton.setText(promotionButton.updateSpan(promoMessage));
            promotionButton.setContentDescription(promoDescription);
        }
    }

    private void buildPromotionButtonIfNeeded() {
        if (promotionButton == null) {
            promotionButton = new PromotionButton(getContext());
            promotionButton.setAffirmTextSize(affirmTextSize);
            promotionButton.setAffirmTextColor(affirmTextColor);
            promotionButton.setTypeface(affirmTextFont > 0
                    ? ResourcesCompat.getFont(getContext(), affirmTextFont) : Typeface.SERIF);
            promotionButton.setAffirmColor(affirmColor);
            promotionButton.setAffirmLogoType(affirmLogoType);
        }
    }

    public boolean isHtmlStyle() {
        return htmlStyling;
    }

    @Deprecated
    public void setAffirmLogoType(@NonNull AffirmLogoType affirmLogoType) {
        this.affirmLogoType = affirmLogoType;
        buildPromotionButtonIfNeeded();
        promotionButton.setAffirmLogoType(affirmLogoType);
    }

    @Deprecated
    public void setAffirmColor(@NonNull AffirmColor affirmColor) {
        this.affirmColor = affirmColor;
        buildPromotionButtonIfNeeded();
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
        promotionWebView = new PromotionWebView(getContext());
        message = null;
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
        promotionButton = new PromotionButton(getContext());
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
        promotionButton.setTypeface(typeface);
        promotionButton.setAffirmTextColor(affirmTextColor);
        promotionButton.setAffirmTextSize(getResources().getDimensionPixelSize(affirmTextSize));
        message = null;
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

    boolean showsPrequal() {
        return showPrequal;
    }

    void setShowPrequal(boolean showPrequal) {
        this.showPrequal = showPrequal;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);

        if (promotionWebView != null) {
            promotionWebView.setWebViewClickListener(l);
        }
    }

    /**
     * Cancels the promo request and releases the WebView used by HTML styling. Called
     * automatically when the view of the host AndroidX Fragment, or the host LifecycleOwner
     * Activity, is destroyed.
     */
    public void destroy() {
        PromoRequestManager.untrack(this);
        clearPromoContent();
        if (promotionWebView != null) {
            promotionWebView.destroyWebView();
            promotionWebView = null;
        }
    }

    /**
     * Shows {@code cached} until the response arrives, or clears the label left by a previous
     * configuration when there is nothing to show.
     */
    void showPlaceholder(@Nullable PromoMessageCache.CachedMessage cached) {
        final String cachedMessage = cached == null ? null : cached.message(htmlStyling);
        if (cached != null && !TextUtils.isEmpty(cachedMessage)) {
            showPrequal = cached.showPrequal;
            setLabel(cachedMessage, cached.ala);
        } else {
            clearPromoContent();
        }
        setVisibility(VISIBLE);
    }

    private void clearPromoContent() {
        message = null;
        showPrequal = false;
        removeAllViews();
        setContentDescription(null);
    }
}

package com.affirm.android;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.res.ResourcesCompat;

import com.affirm.android.model.PromoResponse;

import static com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE;
import static com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO;
import static com.affirm.android.AffirmPromoRenderReason.DETACHED;
import static com.affirm.android.AffirmPromoRenderReason.HOST_DESTROYED;
import static com.affirm.android.AffirmPromoRenderReason.MISSING_HTML;
import static com.affirm.android.AffirmPromoRenderReason.MISSING_NATIVE;
import static com.affirm.android.AffirmPromoRenderReason.NATIVE_ERROR;
import static com.affirm.android.AffirmPromoRenderReason.SUPERSEDED;
import static com.affirm.android.AffirmPromoRenderReason.WEBVIEW_UNAVAILABLE;

public class AffirmPromotionButton extends FrameLayout {

    /** Builds the request of a managed render; called again to restart after reattach. */
    interface PromoRequestFactory {
        @NonNull
        PromoRequest create(@NonNull PromoResponseCallback callback);
    }

    private boolean htmlStyling;
    private String message;
    /** The renderer {@code message} was planned for. */
    private PromoRendererMode messageMode;
    private boolean showPrequal;

    private PromotionButton promotionButton;
    private PromotionWebView promotionWebView;
    /** Explicit and migrated HTML set up the WebView differently, so a switch needs a new one. */
    private boolean promotionWebViewExplicitHtml;

    private AffirmLogoType affirmLogoType;
    private AffirmColor affirmColor;

    private String remoteCssUrl;
    private String typefaceDeclaration;

    private float affirmTextSize;
    private int affirmTextColor;
    private int affirmTextFont;
    private int textGravity;

    @Nullable
    private OnClickListener clickListener;

    @Nullable
    private HtmlAttempt htmlAttempt;

    @Nullable
    private PromoRequestFactory requestFactory;
    @Nullable
    private PromoRender render;
    private boolean restartOnAttach;
    /** How the shown content ended; null while HTML loads or before anything is shown. */
    @Nullable
    private PromoRender.Outcome outcome;
    @Nullable
    private Boolean htmlContentUsable;

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

        textGravity = typedArray.getInt(R.styleable.AffirmPromotionButton_android_gravity,
                Gravity.TOP | Gravity.START);

        affirmLogoType = AffirmLogoType.getAffirmLogoType(affirmLogoTypeOrdinal);
        affirmColor = AffirmColor.getAffirmColor(affirmColorOrdinal);

        typedArray.recycle();
    }

    protected void setLabel(@NonNull String promoMessage, @NonNull String promoDescription) {
        this.message = promoMessage;
        messageMode = htmlStyling ? PromoRendererMode.HTML : PromoRendererMode.NATIVE;
        if (htmlStyling) {
            showHtml(promoMessage, promoDescription, false, true);
        } else {
            showNative(promoMessage, promoDescription, null);
        }
    }

    /** Starts a managed render, cancelling the current one. */
    @MainThread
    void startRender(@NonNull PromoRequestFactory factory) {
        cancelRender(SUPERSEDED);
        restartOnAttach = false;
        requestFactory = factory;
        beginRender();
    }

    private void beginRender() {
        final PromoRender newRender = PromoRender.start(htmlStyling, useLegacyNativeRendering(),
                nativeTypeface());
        render = newRender;
        final PromoRequest request =
                requestFactory.create(new PromoButtonCallback(this, newRender));
        showPlaceholder(PromoMessageCache.get(request.cacheKey()));
        if (!PromoRequestManager.track(this, request)) {
            cancelRender(HOST_DESTROYED);
        }
    }

    private void cancelRender(@NonNull String reason) {
        if (render != null) {
            render.cancel(reason);
            render = null;
        }
        PromoRequestManager.untrack(this);
    }

    /** Shows the response of {@code from} with the inputs captured when it started. */
    void onPromoResponse(@NonNull PromoRender from, @NonNull PromoResponse response,
                         boolean showPrequal) {
        if (from != render) {
            return;
        }
        this.showPrequal = showPrequal;
        final PromoRenderPlan plan = PromoRenderPlan.create(from.explicitHtml, from.optOut,
                from.typeface, response);
        from.onResponse(plan, response.promo().promoConfig().migrateNativeToHtmlRenderStatus());
        render(plan);
        // The content may already be settled, e.g. the same message shown from the cache.
        reportOutcome();
    }

    void onPromoFailure(@NonNull PromoRender from) {
        if (from != render) {
            return;
        }
        if (isEmpty()) {
            setVisibility(GONE);
        }
        from.onFetchFailed();
    }

    /**
     * Shows {@code plan}, unless it is already shown; a failed migrated HTML render falls back
     * to the native copy.
     */
    void render(@NonNull PromoRenderPlan plan) {
        if (!plan.text.isEmpty() && plan.mode == messageMode && plan.text.equals(message)) {
            // Already shown, e.g. from the cache. Reloading would make it flash.
            updateFallback(plan);
            return;
        }
        AffirmLog.d("Promo renders as " + (plan.text.isEmpty() ? "hidden" : plan.mode));
        message = plan.text;
        messageMode = plan.mode;
        outcome = null;
        htmlContentUsable = null;
        if (plan.text.isEmpty()) {
            clearPromoContent();
            setVisibility(GONE);
            settle(PromoRender.Outcome.empty(
                    plan.mode == PromoRendererMode.HTML ? MISSING_HTML : MISSING_NATIVE));
        } else if (plan.mode == PromoRendererMode.HTML) {
            showHtml(plan.text, plan.description, plan.canFallBackToNative, plan.explicitHtml);
        } else {
            showNative(plan.text, plan.description, plan.fallbackReason);
        }
    }

    private void settle(@NonNull PromoRender.Outcome settled) {
        outcome = settled;
        reportOutcome();
    }

    private void reportOutcome() {
        if (render != null && outcome != null) {
            render.onSettled(outcome, htmlContentUsable);
        }
    }

    /** Updates the native copy of the HTML promo already shown without reloading it. */
    private void updateFallback(@NonNull PromoRenderPlan plan) {
        if (htmlAttempt != null) {
            htmlAttempt.description = plan.description;
            htmlAttempt.canFallBackToNative = plan.canFallBackToNative;
            setContentDescription(plan.description);
        } else if (plan.canFallBackToNative && promotionButton != null
                && promotionButton.getParent() == this) {
            // The HTML failed and shows its native fallback; its outcome is already reported.
            promotionButton.setText(promotionButton.updateSpan(plan.description));
            promotionButton.setContentDescription(plan.description);
        }
    }

    /** The font the native promo uses; null means the system default. */
    @Nullable
    Typeface nativeTypeface() {
        buildPromotionButtonIfNeeded();
        return promotionButton.getTypeface();
    }

    private static boolean useLegacyNativeRendering() {
        final AffirmPlugins plugins = AffirmPlugins.get();
        return plugins != null && plugins.useLegacyNativeRendering();
    }

    /** {@code fallbackReason} is what failed in HTML when this is the native fallback. */
    private void showNative(@NonNull String text, @NonNull String description,
                            @Nullable String fallbackReason) {
        clearViews();
        releaseWebView();
        try {
            buildPromotionButtonIfNeeded();
            addView(promotionButton);
            promotionButton.setText(promotionButton.updateSpan(text));
            promotionButton.setContentDescription(description);
        } catch (RuntimeException e) {
            AffirmLog.e("Native promo could not be shown", e);
            clearPromoContent();
            setVisibility(GONE);
            settle(PromoRender.Outcome.failed(NATIVE_ERROR, fallbackReason));
            return;
        }
        settle(PromoRender.Outcome.shown(PromoRendererMode.NATIVE, fallbackReason));
    }

    /** With {@code canFallBackToNative}, a failure before the HTML shows switches to native. */
    private void showHtml(@NonNull String html, @NonNull String description,
                          boolean canFallBackToNative, boolean explicitHtml) {
        clearViews();
        final String css;
        final String pageStyle;
        if (explicitHtml) {
            css = remoteCssUrl;
            pageStyle = typefaceDeclaration;
        } else {
            css = null;
            pageStyle = migratedPageStyle();
        }
        if (promotionWebView == null || explicitHtml != promotionWebViewExplicitHtml) {
            releaseWebView();
            final PromotionWebView created;
            try {
                created = createPromotionWebView();
            } catch (RuntimeException e) {
                // No WebView provider on this device, or it is being updated.
                AffirmLog.e("Promo WebView could not be created", e);
                failHtml(WEBVIEW_UNAVAILABLE, description, canFallBackToNative);
                return;
            }
            created.setWebViewClickListener(clickListener);
            // The button reads the description; the WebView's own nodes would repeat it.
            created.setFocusable(false);
            created.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            if (!explicitHtml) {
                created.getSettings().setTextZoom(100);
                created.setShrinkToContentWidth(true);
            }
            addView(created);
            promotionWebView = created;
            promotionWebViewExplicitHtml = explicitHtml;
        }
        final PromotionWebView webView = promotionWebView;
        setContentDescription(description);
        // Below API 26 a clickable view is not focusable on its own, so keys could not reach it.
        setFocusable(true);
        htmlAttempt = new HtmlAttempt(description, canFallBackToNative);
        webView.setRenderListener(htmlAttempt);
        postDelayed(htmlAttempt, AffirmPromoRendering.HTML_TIMEOUT_MS);
        webView.loadWebData(html, css, pageStyle);
    }

    /** The page style a migrated HTML render uses, from the current native text style. */
    @Nullable
    private String migratedPageStyle() {
        buildPromotionButtonIfNeeded();
        return PromoHtmlStyle.migratedCss(promotionButton.getTypeface(),
                promotionButton.getCurrentTextColor(), promotionButton.getTextSize(),
                promotionButton.getGravity(), getResources().getDisplayMetrics().density);
    }

    /** Throws when the device has no usable WebView provider. */
    @VisibleForTesting
    @NonNull
    PromotionWebView createPromotionWebView() {
        return new PromotionWebView(getContext());
    }

    private void failHtml(@NonNull String reason, @NonNull String description,
                          boolean canFallBackToNative) {
        if (canFallBackToNative) {
            showNative(description, description, reason);
        } else {
            clearPromoContent();
            setVisibility(GONE);
            settle(PromoRender.Outcome.failed(reason, null));
        }
    }

    /**
     * Removes what is shown, keeping {@code message}. The WebView stays loaded but hidden, so
     * the next HTML promo, e.g. on a recycled list row, does not load a new page.
     */
    private void clearViews() {
        if (htmlAttempt != null) {
            removeCallbacks(htmlAttempt);
            htmlAttempt = null;
        }
        if (promotionButton != null && promotionButton.getParent() == this) {
            removeView(promotionButton);
        }
        setContentDescription(null);
        // Native text is focused through its own button.
        setFocusable(false);
        if (promotionWebView != null) {
            promotionWebView.hidePromo();
        }
    }

    private void releaseWebView() {
        if (promotionWebView != null) {
            promotionWebView.destroyWebView();
            promotionWebView = null;
        }
    }

    /** Watches one HTML load; also the timeout, since the page can't report a stalled load. */
    private final class HtmlAttempt implements PromotionWebView.RenderListener, Runnable {
        private String description;
        private boolean canFallBackToNative;
        private boolean ready;

        HtmlAttempt(@NonNull String description, boolean canFallBackToNative) {
            this.description = description;
            this.canFallBackToNative = canFallBackToNative;
        }

        @Override
        public void onSanitized(boolean usable) {
            if (htmlAttempt == this) {
                htmlContentUsable = usable;
            }
        }

        @Override
        public void onReady() {
            ready = true;
            removeCallbacks(this);
            AffirmLog.d("Promo HTML is showing");
            if (htmlAttempt == this) {
                settle(PromoRender.Outcome.shown(PromoRendererMode.HTML, null));
            }
        }

        @Override
        public void onFailure(@NonNull String reason) {
            removeCallbacks(this);
            // Not inside the WebView's own callback, since this destroys it.
            post(() -> failIfCurrent(reason));
        }

        @Override
        public void run() {
            failIfCurrent(AffirmPromoRenderReason.HTML_TIMEOUT);
        }

        private void failIfCurrent(@NonNull String reason) {
            if (htmlAttempt != this) {
                AffirmLog.d("Ignoring " + reason + " from a replaced promo HTML view");
                return;
            }
            // Once the HTML has shown, switching to native would look like a second promo.
            final boolean toNative = canFallBackToNative && !ready;
            AffirmLog.w("Promo HTML failed (" + reason + "), "
                    + (toNative ? "showing native" : "hiding"));
            // The page or its renderer may be broken; the next HTML promo starts over.
            releaseWebView();
            failHtml(reason, description, toNative);
        }
    }

    private void buildPromotionButtonIfNeeded() {
        if (promotionButton == null) {
            promotionButton = new PromotionButton(getContext());
            promotionButton.setGravity(textGravity);
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

    /** Aligns the promo text, like {@code android:gravity}. Call before the promo loads. */
    public void setTextGravity(int gravity) {
        buildPromotionButtonIfNeeded();
        promotionButton.setGravity(gravity);
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
        if (!htmlStyling
                || !TextUtils.equals(remoteCssUrl, this.remoteCssUrl)
                || !TextUtils.equals(typefaceDeclaration, this.typefaceDeclaration)) {
            // Re-render with the new style even if the response is the same. A migrated HTML
            // render shows the same copy with a different style and fallback.
            message = null;
        }
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
        final boolean wasHtmlStyling = this.htmlStyling;
        this.htmlStyling = false;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        final String previousPageStyle = migratedPageStyle();
        promotionButton.setAffirmColor(affirmColor);
        promotionButton.setAffirmLogoType(affirmLogoType);
        promotionButton.setTypeface(typeface);
        promotionButton.setAffirmTextColor(affirmTextColor);
        promotionButton.setAffirmTextSize(getResources().getDimensionPixelSize(affirmTextSize));
        // Re-render with the new style even if the response is the same. Native is cheap; a
        // migrated HTML render only reloads when its page style changes, so rebinding a list
        // row with the same style does not flash.
        if (wasHtmlStyling || promotionButton.getParent() == this
                || !TextUtils.equals(previousPageStyle, migratedPageStyle())) {
            message = null;
        }
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

    /** Taps before the promo is on screen do nothing. */
    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l == null ? null : v -> {
            if (isContentShown()) {
                l.onClick(v);
            }
        });
        clickListener = l;

        if (promotionWebView != null) {
            promotionWebView.setWebViewClickListener(l);
        }
    }

    /** TalkBack announces an HTML promo as a button, like the native one. */
    @Override
    public CharSequence getAccessibilityClassName() {
        return isFocusable() ? Button.class.getName() : super.getAccessibilityClassName();
    }

    @VisibleForTesting
    @Nullable
    PromotionWebView getPromotionWebView() {
        return promotionWebView;
    }

    /**
     * Cancels the promo request and releases the WebView used by HTML styling. Called
     * automatically when the view of the host AndroidX Fragment, or the host LifecycleOwner
     * Activity, is destroyed.
     */
    public void destroy() {
        cancelRender(HOST_DESTROYED);
        requestFactory = null;
        restartOnAttach = false;
        clearPromoContent();
        releaseWebView();
    }

    /** An unfinished render is cancelled on detach and started again on reattach. */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        final PromoRender detached = render;
        if (detached != null && !detached.isEnded()) {
            // Deferred: a Fragment's view is removed before its view lifecycle is destroyed,
            // and a view moved within one pass is detached and attached again right away.
            // post() still reaches the window's handler here, not the queue run on reattach.
            post(() -> cancelIfStillDetached(detached));
        }
    }

    private void cancelIfStillDetached(@NonNull PromoRender detached) {
        if (detached != render || detached.isEnded() || isAttachedToWindow()) {
            return;
        }
        cancelRender(DETACHED);
        clearPromoContent();
        restartOnAttach = true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (restartOnAttach && requestFactory != null) {
            restartOnAttach = false;
            beginRender();
        }
    }

    private boolean isContentShown() {
        if (promotionButton != null && promotionButton.getParent() == this) {
            return true;
        }
        return promotionWebView != null && promotionWebView.getParent() == this
                && promotionWebView.isPromoShown();
    }

    /**
     * Shows {@code cached} until the response arrives, or clears the label left by a previous
     * configuration when there is nothing to show.
     */
    void showPlaceholder(@Nullable PromoMessageCache.CachedMessage cached) {
        final PromoRenderPlan plan = cached == null ? null : PromoRenderPlan.create(htmlStyling,
                useLegacyNativeRendering(), nativeTypeface(), cached.ala, cached.htmlAla,
                cached.migrateToHtml);
        setVisibility(VISIBLE);
        if (plan != null && !plan.text.isEmpty()) {
            showPrequal = cached.showPrequal;
            render(plan);
        } else {
            clearPromoContent();
        }
    }

    private void clearPromoContent() {
        message = null;
        showPrequal = false;
        outcome = null;
        clearViews();
    }
}

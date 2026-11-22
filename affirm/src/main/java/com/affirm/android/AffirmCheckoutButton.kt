package com.affirm.android

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Layout
import android.text.SpannableString
import android.text.TextPaint
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.affirm.android.AffirmCheckoutButtonShape.PILL
import com.affirm.android.AffirmCheckoutButtonShape.ROUNDED
import com.affirm.android.AffirmCheckoutButtonShape.SHARP
import com.affirm.android.AffirmCheckoutButtonSize.LARGE
import com.affirm.android.AffirmCheckoutButtonSize.MEDIUM
import com.affirm.android.AffirmCheckoutButtonSize.SMALL
import com.affirm.android.AffirmCheckoutButtonTextType.BUY_NOW
import com.affirm.android.AffirmCheckoutButtonTextType.CHECKOUT
import com.affirm.android.AffirmCheckoutButtonTextType.LOGO_ONLY
import com.affirm.android.AffirmCheckoutButtonTheme.DARK
import com.affirm.android.AffirmCheckoutButtonTheme.LIGHT
import com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_BLUE_BLACK
import com.affirm.android.AffirmColor.AFFIRM_COLOR_TYPE_WHITE
import com.affirm.android.AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO
import com.affirm.android.AffirmTracker.TrackingEvent.CHECKOUT_BUTTON_CLICKED
import com.affirm.android.AffirmTracker.TrackingLevel.INFO
import com.affirm.android.model.PromoPageType
import com.google.gson.JsonObject
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.properties.Delegates

/**
 * A pre-styled Affirm checkout button.
 *
 * It does not start checkout: set an [OnClickListener] that does. It does not disable itself
 * either; call `setEnabled(false)` when starting checkout and re-enable it when the request
 * completes. While disabled, it is drawn faded.
 *
 * It always shows one line: if the text and logo are too wide, for example at a large font
 * scale, they shrink to fit.
 */
class AffirmCheckoutButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    // No default button style: the app theme's tint, caps and elevation would restyle it.
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    var pageType: PromoPageType? = null

    var size: AffirmCheckoutButtonSize by restyling(MEDIUM)

    var theme: AffirmCheckoutButtonTheme by restyling(DARK)

    var shape: AffirmCheckoutButtonShape by restyling(PILL)

    var buttonText: AffirmCheckoutButtonTextType by restyling(BUY_NOW)

    var isFullWidth: Boolean by Delegates.observable(false) { _, old, new ->
        if (old != new) requestLayout()
    }

    // The size the spec sets; onMeasure shrinks the text below it when one line is too narrow.
    private var styledTextSize = 0f
    private val measuringPaint = TextPaint()

    // Styling waits until init has read every attribute, so it runs once.
    private var initialized = false

    init {
        gravity = Gravity.CENTER
        maxLines = 1
        isAllCaps = false
        isClickable = true
        isFocusable = true

        val a = context.obtainStyledAttributes(
            attrs, R.styleable.AffirmCheckoutButton, defStyleAttr, 0
        )
        if (a.hasValue(R.styleable.AffirmCheckoutButton_affirmPageType)) {
            pageType = PromoPageType.values()[
                a.getInt(R.styleable.AffirmCheckoutButton_affirmPageType, 0)
            ]
        }
        size = AffirmCheckoutButtonSize.values()[
            a.getInt(R.styleable.AffirmCheckoutButton_affirmButtonSize, size.ordinal)
        ]
        theme = AffirmCheckoutButtonTheme.values()[
            a.getInt(R.styleable.AffirmCheckoutButton_affirmButtonTheme, theme.ordinal)
        ]
        shape = AffirmCheckoutButtonShape.values()[
            a.getInt(R.styleable.AffirmCheckoutButton_affirmButtonShape, shape.ordinal)
        ]
        buttonText = AffirmCheckoutButtonTextType.values()[
            a.getInt(R.styleable.AffirmCheckoutButton_affirmButtonText, buttonText.ordinal)
        ]
        isFullWidth = a.getBoolean(R.styleable.AffirmCheckoutButton_affirmFullWidth, isFullWidth)
        a.recycle()
        initialized = true
        applyStyle()
    }

    private fun <T> restyling(initial: T) =
        Delegates.observable(initial) { _, old, new ->
            if (initialized && old != new) applyStyle()
        }

    private fun applyStyle() {
        val (textSizeSp, paddingDp) = when (size) {
            SMALL -> 16 to 16
            MEDIUM -> 18 to 24
            LARGE -> 20 to 28
        }
        val (backgroundColorRes, textColorRes, logoColor) = when (theme) {
            DARK -> Triple(
                R.color.affirm_checkout_button_bg, R.color.affirm_checkout_button_text,
                AFFIRM_COLOR_TYPE_WHITE
            )
            LIGHT -> Triple(
                R.color.affirm_white, R.color.affirm_checkout_button_light_text,
                AFFIRM_COLOR_TYPE_BLUE_BLACK
            )
        }
        val (textRes, descriptionRes) = when (buttonText) {
            BUY_NOW -> R.string.checkout_button_buy_now to R.string.checkout_button_buy_now
            CHECKOUT -> R.string.checkout_button_checkout to R.string.checkout_button_checkout
            LOGO_ONLY ->
                R.string.checkout_button_logo_only to R.string.checkout_button_logo_only_description
        }

        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, textSizeSp.toFloat(), resources.displayMetrics
        )
        val padding = dpToPx(paddingDp)
        styledTextSize = textSize
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        setPadding(padding, 0, padding, 0)
        val textColor = ContextCompat.getColor(context, textColorRes)
        setTextColor(textColor)
        val title = AffirmUtils.createSpannableForText(
            context.getString(textRes), textSize, AFFIRM_DISPLAY_TYPE_LOGO, logoColor, context
        )
        text = title.withScalingLogo(centered = buttonText == LOGO_ONLY)
        contentDescription = context.getString(descriptionRes)

        val radius = when (shape) {
            SHARP -> 0f
            PILL -> heightPx() / 2f
            ROUNDED -> resources.getDimension(R.dimen.affirm_corner_radius)
        }
        val backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
        // Without a mask layer, the ripple is clipped to the rounded fill.
        background = RippleDrawable(
            ColorStateList.valueOf(ColorUtils.setAlphaComponent(textColor, RIPPLE_ALPHA)),
            GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = radius
            },
            null
        )
        requestLayout()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        applyEnabledAlpha()
    }

    // Fades the logo span too, which a text color state list would not.
    // TextView's constructor calls setEnabled() for android:enabled, before this class's
    // fields are initialized: read only View state here.
    private fun applyEnabledAlpha() {
        alpha = if (isEnabled) 1f else DISABLED_ALPHA
    }

    private fun heightPx(): Int = dpToPx(
        when (size) {
            SMALL, MEDIUM -> 48
            LARGE -> 64
        }
    )

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).roundToInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width =
            if (isFullWidth && MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
                MeasureSpec.makeMeasureSpec(
                    MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY
                )
            } else {
                widthMeasureSpec
            }
        if (MeasureSpec.getMode(width) != MeasureSpec.UNSPECIFIED) {
            fitToOneLine(MeasureSpec.getSize(width) - paddingLeft - paddingRight)
        }
        super.onMeasure(width, MeasureSpec.makeMeasureSpec(heightPx(), MeasureSpec.EXACTLY))
    }

    // Large font scales can make the text and logo wider than the button can be. Rather than
    // dropping the logo onto a hidden second line, shrink both until they fit on one.
    private fun fitToOneLine(availableWidth: Int) {
        measuringPaint.set(paint)
        fun widthAt(textSizePx: Float): Float {
            measuringPaint.textSize = textSizePx
            return Layout.getDesiredWidth(text, measuringPaint)
        }

        val styledWidth = widthAt(styledTextSize)
        var fittedSize = styledTextSize
        if (styledWidth > availableWidth && availableWidth > 0) {
            fittedSize = floor(styledTextSize * availableWidth / styledWidth)
            while (fittedSize > 1f && widthAt(fittedSize) > availableWidth) {
                fittedSize--
            }
        }
        if (fittedSize != textSize) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, fittedSize)
        }
    }

    override fun performClick(): Boolean {
        // Tracking reads the SDK configuration; the button can be shown before initialize().
        if (AffirmPlugins.get() != null) {
            AffirmTracker.track(CHECKOUT_BUTTON_CLICKED, INFO, clickTrackingData())
        }
        return super.performClick()
    }

    private fun clickTrackingData() = JsonObject().apply {
        addProperty("page_type", pageType?.type ?: "")
        addProperty("button_size", size.trackingName)
        addProperty("button_theme", theme.trackingName)
        addProperty("button_shape", shape.trackingName)
        addProperty("button_text", buttonText.trackingName)
    }

    // The values AFJS reports, e.g. BUY_NOW -> "buy-now".
    private val Enum<*>.trackingName: String
        get() = name.lowercase(Locale.ROOT).replace('_', '-')

    private fun SpannableString.withScalingLogo(centered: Boolean) = apply {
        for (span in getSpans(0, length, ImageSpan::class.java)) {
            val start = getSpanStart(span)
            val end = getSpanEnd(span)
            removeSpan(span)
            setSpan(LogoSpan(span.drawable, centered), start, end, SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * The logo, as tall as the text: it follows the text size, so it shrinks with the text in
     * [fitToOneLine]. Next to text it sits on the baseline; alone, baseline alignment would put
     * it above the center by the font's descent, so it is centered in the line instead.
     */
    private class LogoSpan(
        logo: Drawable,
        private val centered: Boolean
    ) : ImageSpan(logo, ALIGN_BASELINE) {

        private val aspectRatio = logo.intrinsicWidth.toFloat() / logo.intrinsicHeight

        override fun getSize(
            paint: Paint,
            text: CharSequence?,
            start: Int,
            end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            resizeTo(paint)
            return super.getSize(paint, text, start, end, fm)
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            resizeTo(paint)
            if (!centered) {
                super.draw(canvas, text, start, end, x, top, y, bottom, paint)
                return
            }
            canvas.save()
            canvas.translate(x, (top + bottom - drawable.bounds.height()) / 2f)
            drawable.draw(canvas)
            canvas.restore()
        }

        private fun resizeTo(paint: Paint) {
            val height = paint.textSize.roundToInt()
            drawable.setBounds(0, 0, (height * aspectRatio).roundToInt(), height)
        }
    }

    private companion object {
        // Pressed overlay, strong enough to show on the dark fill.
        private const val RIPPLE_ALPHA = 0x33
        // Material's opacity for a disabled component.
        private const val DISABLED_ALPHA = 0.38f
    }
}

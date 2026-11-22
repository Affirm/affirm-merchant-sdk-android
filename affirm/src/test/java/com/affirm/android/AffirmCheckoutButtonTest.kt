package com.affirm.android

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View.MeasureSpec
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import com.affirm.android.model.PromoPageType
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AffirmCheckoutButtonTest {

    private val tracked = mutableListOf<JsonObject>()
    private val defaultSender = AffirmTracker.sender
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        initializeAffirm()
        AffirmTracker.sender = AffirmTracker.Sender { tracked.add(it) }
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @After
    fun tearDown() {
        AffirmTracker.sender = defaultSender
    }

    @Test
    fun defaults() {
        val button = AffirmCheckoutButton(activity)
        activity.setContentView(button)

        assertThat(button.pageType).isNull()
        assertThat(button.size).isEqualTo(AffirmCheckoutButtonSize.MEDIUM)
        assertThat(button.theme).isEqualTo(AffirmCheckoutButtonTheme.DARK)
        assertThat(button.shape).isEqualTo(AffirmCheckoutButtonShape.PILL)
        assertThat(button.buttonText).isEqualTo(AffirmCheckoutButtonTextType.BUY_NOW)
        assertThat(button.isFullWidth).isFalse()
        assertThat(button.isClickable).isTrue()
        assertText(button, "Buy now with Affirm", "Buy now with Affirm")
    }

    @Test
    fun sizes() {
        assertSize(AffirmCheckoutButtonSize.SMALL, heightDp = 48, textSizeSp = 16, paddingDp = 16)
        assertSize(AffirmCheckoutButtonSize.MEDIUM, heightDp = 48, textSizeSp = 18, paddingDp = 24)
        assertSize(AffirmCheckoutButtonSize.LARGE, heightDp = 64, textSizeSp = 20, paddingDp = 28)
    }

    @Test
    fun themes() {
        val button = button()
        assertThat(button.backgroundColor()).isEqualTo(Color.parseColor("#000049"))
        assertThat(button.currentTextColor).isEqualTo(Color.parseColor("#F7F7F8"))
        // The white logo is the black one tinted; the blue-black logo is drawn as is.
        assertThat(button.logoSpans().single().drawable.colorFilter).isNotNull()

        button.theme = AffirmCheckoutButtonTheme.LIGHT
        assertThat(button.backgroundColor()).isEqualTo(Color.WHITE)
        assertThat(button.currentTextColor).isEqualTo(Color.parseColor("#0C0C14"))
        assertThat(button.logoSpans().single().drawable.colorFilter).isNull()
    }

    @Test
    fun shapes() {
        val button = button()
        button.size = AffirmCheckoutButtonSize.LARGE

        button.shape = AffirmCheckoutButtonShape.SHARP
        assertThat(button.backgroundDrawable().cornerRadius).isWithin(TOLERANCE).of(0f)

        button.shape = AffirmCheckoutButtonShape.ROUNDED
        assertThat(button.backgroundDrawable().cornerRadius).isWithin(TOLERANCE).of(dp(8))

        button.shape = AffirmCheckoutButtonShape.PILL
        assertThat(button.backgroundDrawable().cornerRadius).isWithin(TOLERANCE).of(dp(32))
    }

    @Test
    fun pressedRippleIsClippedToTheFill() {
        val button = button()
        for (shape in AffirmCheckoutButtonShape.values()) {
            button.shape = shape
            val ripple = button.background as RippleDrawable
            assertThat(ripple.numberOfLayers).isEqualTo(1)
            assertThat(ripple.getDrawable(0)).isInstanceOf(GradientDrawable::class.java)
        }
    }

    @Test
    fun disabledFades() {
        val button = button()
        assertThat(button.alpha).isEqualTo(1f)

        button.isEnabled = false
        assertThat(button.alpha).isWithin(TOLERANCE).of(0.38f)

        button.isEnabled = true
        assertThat(button.alpha).isEqualTo(1f)
    }

    @Test
    fun disabledFromXmlFades() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(android.R.attr.enabled, "false")
            .build()

        val button = button(attrs)

        assertThat(button.isEnabled).isFalse()
        assertThat(button.alpha).isWithin(TOLERANCE).of(0.38f)
    }

    @Test
    fun textTypes() {
        val button = button()

        button.buttonText = AffirmCheckoutButtonTextType.CHECKOUT
        assertText(button, "Checkout with Affirm", "Checkout with Affirm")

        button.buttonText = AffirmCheckoutButtonTextType.LOGO_ONLY
        assertText(button, "Affirm", "Buy now with Affirm")
    }

    @Test
    fun logoOnlyIsCenteredInTheLine() {
        val button = button()
        button.buttonText = AffirmCheckoutButtonTextType.LOGO_ONLY
        val logo = button.logoSpans().single()
        val canvas = mock(Canvas::class.java)

        logo.draw(canvas, button.text, 0, button.text.length, 5f, 10, 80, 110, button.paint)

        verify(canvas).translate(5f, (10 + 110 - logo.drawable.bounds.height()) / 2f)
    }

    @Test
    fun logoNextToTextStaysOnTheBaseline() {
        val button = button()
        for (text in listOf(
            AffirmCheckoutButtonTextType.BUY_NOW, AffirmCheckoutButtonTextType.CHECKOUT
        )) {
            button.buttonText = text
            val logo = button.logoSpans().single()
            val canvas = mock(Canvas::class.java)

            logo.draw(canvas, button.text, 0, button.text.length, 5f, 10, 80, 110, button.paint)

            val descent = button.paint.fontMetricsInt.descent
            verify(canvas).translate(5f, (110 - logo.drawable.bounds.height() - descent).toFloat())
        }
    }

    @Test
    fun shrinksWhenOneLineIsTooNarrow() {
        val button = button()
        button.size = AffirmCheckoutButtonSize.LARGE
        button.buttonText = AffirmCheckoutButtonTextType.CHECKOUT
        val tooNarrow = button.paddingLeft + button.paddingRight + 1

        button.measure(MeasureSpec.makeMeasureSpec(tooNarrow, MeasureSpec.AT_MOST))

        assertThat(button.textSize).isLessThan(sp(20))
        assertThat(button.textSize).isAtLeast(1f)
    }

    @Test
    fun returnsToTheStyledSizeWhenItFits() {
        val button = button()
        button.size = AffirmCheckoutButtonSize.LARGE
        val tooNarrow = button.paddingLeft + button.paddingRight + 1
        button.measure(MeasureSpec.makeMeasureSpec(tooNarrow, MeasureSpec.AT_MOST))

        button.measure(MeasureSpec.makeMeasureSpec(dp(1000).roundToInt(), MeasureSpec.AT_MOST))

        assertThat(button.textSize).isWithin(TOLERANCE).of(sp(20))
    }

    @Test
    fun logoFollowsTheTextSize() {
        val button = button()
        val logo = button.logoSpans().single()

        logo.getSize(TextPaint().apply { textSize = 10f }, button.text, 0, button.text.length, null)

        assertThat(logo.drawable.bounds.height()).isEqualTo(10)
    }

    @Test
    fun everyCombinationRenders() {
        val button = button()
        for (size in AffirmCheckoutButtonSize.values()) {
            for (theme in AffirmCheckoutButtonTheme.values()) {
                for (shape in AffirmCheckoutButtonShape.values()) {
                    for (text in AffirmCheckoutButtonTextType.values()) {
                        button.size = size
                        button.theme = theme
                        button.shape = shape
                        button.buttonText = text
                        button.measure(MeasureSpec.UNSPECIFIED)

                        assertThat(button.measuredHeight).isAtLeast(dp(48).roundToInt())
                        assertThat(button.logoSpans()).hasLength(1)
                    }
                }
            }
        }
    }

    @Test
    fun fullWidth() {
        val button = button()
        val parentWidth = dp(1000).roundToInt()

        button.measure(MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST))
        assertThat(button.measuredWidth).isLessThan(parentWidth)

        button.isFullWidth = true
        button.measure(MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.AT_MOST))
        assertThat(button.measuredWidth).isEqualTo(parentWidth)
    }

    @Test
    fun fullWidthWrapsWhenTheWidthIsUnbounded() {
        val button = button()
        button.measure(MeasureSpec.UNSPECIFIED)
        val wrapped = button.measuredWidth

        button.isFullWidth = true
        button.measure(MeasureSpec.UNSPECIFIED)

        assertThat(button.measuredWidth).isEqualTo(wrapped)
    }

    @Test
    fun heightComesFromTheSizeOnly() {
        val button = button()

        button.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(dp(200).roundToInt(), MeasureSpec.EXACTLY)
        )

        assertThat(button.measuredHeight).isEqualTo(dp(48).roundToInt())
    }

    @Test
    fun xmlAttributes() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.affirmPageType, "product")
            .addAttribute(R.attr.affirmButtonSize, "large")
            .addAttribute(R.attr.affirmButtonTheme, "light")
            .addAttribute(R.attr.affirmButtonShape, "rounded")
            .addAttribute(R.attr.affirmButtonText, "logoOnly")
            .addAttribute(R.attr.affirmFullWidth, "true")
            .build()

        val button = button(attrs)

        assertThat(button.pageType).isEqualTo(PromoPageType.PRODUCT)
        assertThat(button.size).isEqualTo(AffirmCheckoutButtonSize.LARGE)
        assertThat(button.theme).isEqualTo(AffirmCheckoutButtonTheme.LIGHT)
        assertThat(button.shape).isEqualTo(AffirmCheckoutButtonShape.ROUNDED)
        assertThat(button.buttonText).isEqualTo(AffirmCheckoutButtonTextType.LOGO_ONLY)
        assertThat(button.isFullWidth).isTrue()
        assertText(button, "Affirm", "Buy now with Affirm")
    }

    @Test
    fun xmlEnumValuesMatchTheEnums() {
        val pageTypes = listOf(
            "banner", "cart", "category", "homepage", "landing", "payment", "product", "search"
        )
        assertThat(PromoPageType.values()).hasLength(pageTypes.size)
        assertXmlValues(R.attr.affirmPageType, pageTypes.associateWith { it }) {
            it.pageType!!.type
        }
        assertXmlValues(
            R.attr.affirmButtonSize,
            mapOf(
                "small" to AffirmCheckoutButtonSize.SMALL,
                "medium" to AffirmCheckoutButtonSize.MEDIUM,
                "large" to AffirmCheckoutButtonSize.LARGE
            )
        ) { it.size }
        assertXmlValues(
            R.attr.affirmButtonTheme,
            mapOf(
                "dark" to AffirmCheckoutButtonTheme.DARK,
                "light" to AffirmCheckoutButtonTheme.LIGHT
            )
        ) { it.theme }
        assertXmlValues(
            R.attr.affirmButtonShape,
            mapOf(
                "sharp" to AffirmCheckoutButtonShape.SHARP,
                "pill" to AffirmCheckoutButtonShape.PILL,
                "rounded" to AffirmCheckoutButtonShape.ROUNDED
            )
        ) { it.shape }
        assertXmlValues(
            R.attr.affirmButtonText,
            mapOf(
                "buyNow" to AffirmCheckoutButtonTextType.BUY_NOW,
                "checkout" to AffirmCheckoutButtonTextType.CHECKOUT,
                "logoOnly" to AffirmCheckoutButtonTextType.LOGO_ONLY
            )
        ) { it.buttonText }
    }

    @Test
    fun ignoresTheAppButtonStyle() {
        val themed = ContextThemeWrapper(
            activity, androidx.appcompat.R.style.Theme_AppCompat_Light
        )
        assertThat(AppCompatButton(themed).transformationMethod).isNotNull()

        val button = AffirmCheckoutButton(themed)

        assertThat(button.transformationMethod).isNull()
        assertThat(button.stateListAnimator).isNull()
        assertThat(button.backgroundTintList).isNull()
        assertThat(button.backgroundColor()).isEqualTo(Color.parseColor("#000049"))
    }

    @Test
    fun ignoresTextAllCapsFromXml() {
        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(android.R.attr.textAllCaps, "true")
            .build()

        val button = button(attrs)

        assertThat(button.transformationMethod).isNull()
        assertText(button, "Buy now with Affirm", "Buy now with Affirm")
    }

    @Test
    fun shrinkingSettlesAfterOneLayout() {
        for (mode in listOf(MeasureSpec.EXACTLY, MeasureSpec.AT_MOST)) {
            val parent = FrameLayout(activity)
            val button = AffirmCheckoutButton(activity)
            button.size = AffirmCheckoutButtonSize.LARGE
            button.buttonText = AffirmCheckoutButtonTextType.CHECKOUT
            parent.addView(button)
            val tooNarrow = MeasureSpec.makeMeasureSpec(
                button.paddingLeft + button.paddingRight + 1, mode
            )

            button.measure(tooNarrow)
            parent.layout(0, 0, dp(1000).roundToInt(), dp(1000).roundToInt())
            val fitted = button.textSize
            assertThat(fitted).isLessThan(sp(20))
            assertThat(parent.isLayoutRequested).isFalse()

            // Measuring again with the same width must not change the size or relayout.
            button.forceLayout()
            button.measure(tooNarrow)
            assertThat(button.textSize).isEqualTo(fitted)
            assertThat(parent.isLayoutRequested).isFalse()

            button.measure(MeasureSpec.makeMeasureSpec(dp(1000).roundToInt(), mode))
            assertThat(button.textSize).isWithin(TOLERANCE).of(sp(20))
        }
    }

    @Test
    fun clickTracksBeforeTheListener() {
        val button = button()
        val trackedAtClick = mutableListOf<Int>()
        button.setOnClickListener { trackedAtClick.add(tracked.size) }

        assertThat(button.performClick()).isTrue()

        assertThat(trackedAtClick).containsExactly(1)
        val event = tracked.single()
        assertThat(event.get("event_name").asString).isEqualTo("Checkout button clicked")
        assertThat(event.get("level").asString).isEqualTo("info")
    }

    @Test
    fun clickTrackingData() {
        val button = button()
        assertTracking(button, "medium", "dark", "pill", "buy-now")

        button.size = AffirmCheckoutButtonSize.SMALL
        button.theme = AffirmCheckoutButtonTheme.LIGHT
        button.shape = AffirmCheckoutButtonShape.SHARP
        button.buttonText = AffirmCheckoutButtonTextType.LOGO_ONLY
        assertTracking(button, "small", "light", "sharp", "logo-only")

        button.size = AffirmCheckoutButtonSize.LARGE
        button.shape = AffirmCheckoutButtonShape.ROUNDED
        button.buttonText = AffirmCheckoutButtonTextType.CHECKOUT
        assertTracking(button, "large", "light", "rounded", "checkout")
    }

    @Test
    fun clickTracksThePageType() {
        val button = button()
        button.performClick()
        assertThat(tracked.last().get("page_type").asString).isEmpty()

        button.pageType = PromoPageType.PRODUCT
        button.performClick()
        assertThat(tracked.last().get("page_type").asString).isEqualTo("product")

        val attrs = Robolectric.buildAttributeSet()
            .addAttribute(R.attr.affirmPageType, "cart")
            .build()
        button(attrs).performClick()
        assertThat(tracked.last().get("page_type").asString).isEqualTo("cart")
    }

    @Test
    fun clickBeforeInitializeDoesNotCrash() {
        val button = button()
        var clicks = 0
        button.setOnClickListener { clicks++ }

        AffirmPlugins.reset()
        try {
            assertThat(button.performClick()).isTrue()
        } finally {
            initializeAffirm()
        }

        assertThat(clicks).isEqualTo(1)
        assertThat(tracked).isEmpty()
    }

    private fun initializeAffirm() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            )
        }
    }

    // Attached, as in an app: TextView needs layout params to relayout after a measure.
    private fun button(attrs: AttributeSet? = null) =
        AffirmCheckoutButton(activity, attrs).also { activity.setContentView(it) }

    private fun assertSize(
        size: AffirmCheckoutButtonSize,
        heightDp: Int,
        textSizeSp: Int,
        paddingDp: Int
    ) {
        val button = button()
        button.size = size
        button.measure(MeasureSpec.UNSPECIFIED)

        assertThat(button.measuredHeight).isEqualTo(dp(heightDp).roundToInt())
        assertThat(button.textSize).isWithin(TOLERANCE).of(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSizeSp.toFloat(),
                activity.resources.displayMetrics
            )
        )
        assertThat(button.paddingLeft).isEqualTo(dp(paddingDp).roundToInt())
        assertThat(button.paddingRight).isEqualTo(dp(paddingDp).roundToInt())
    }

    private fun assertText(button: AffirmCheckoutButton, text: String, description: String) {
        assertThat(button.text.toString()).isEqualTo(text)
        assertThat(button.contentDescription.toString()).isEqualTo(description)
        val logo = button.logoSpans().single()
        assertThat((button.text as Spanned).getSpanStart(logo)).isEqualTo(text.indexOf("Affirm"))
    }

    private fun assertTracking(
        button: AffirmCheckoutButton,
        size: String,
        theme: String,
        shape: String,
        text: String
    ) {
        button.performClick()
        val data = tracked.last()
        assertThat(data.get("button_size").asString).isEqualTo(size)
        assertThat(data.get("button_theme").asString).isEqualTo(theme)
        assertThat(data.get("button_shape").asString).isEqualTo(shape)
        assertThat(data.get("button_text").asString).isEqualTo(text)
    }

    private fun <T> assertXmlValues(
        attr: Int,
        expected: Map<String, T>,
        actual: (AffirmCheckoutButton) -> T
    ) {
        for ((xmlValue, value) in expected) {
            val attrs = Robolectric.buildAttributeSet().addAttribute(attr, xmlValue).build()
            assertThat(actual(AffirmCheckoutButton(activity, attrs))).isEqualTo(value)
        }
    }

    private fun AffirmCheckoutButton.measure(widthMeasureSpec: Int) =
        measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))

    private fun AffirmCheckoutButton.logoSpans(): Array<ImageSpan> =
        (text as Spanned).getSpans(0, text.length, ImageSpan::class.java)

    private fun AffirmCheckoutButton.backgroundDrawable() =
        (background as RippleDrawable).getDrawable(0) as GradientDrawable

    private fun AffirmCheckoutButton.backgroundColor() =
        backgroundDrawable().color!!.defaultColor

    private fun dp(value: Int): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), activity.resources.displayMetrics
    )

    private fun sp(value: Int): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, value.toFloat(), activity.resources.displayMetrics
    )

    private companion object {
        const val TOLERANCE = 0.01f
    }
}

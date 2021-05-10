package com.affirm.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.affirm.android.R
import kotlin.math.roundToInt

open class FixedAspectRatioFrameLayout(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    protected var aspectRatioWidth: Int = 0
    private var aspectRatioHeight: Int = 0
    private var aspectRatioFixedWidth: Boolean = false

    init {
        attrs?.let { attributes ->
            val a = context.obtainStyledAttributes(attributes, R.styleable.FixedAspectRatioFrameLayout)

            aspectRatioWidth = a.getInt(R.styleable.FixedAspectRatioFrameLayout_aspectRatioWidth, 1)
            aspectRatioHeight = a.getInt(R.styleable.FixedAspectRatioFrameLayout_aspectRatioHeight, 1)
            aspectRatioFixedWidth = a.getBoolean(
                R.styleable.FixedAspectRatioFrameLayout_aspectRatioFixedWidth, true)

            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val originalHeight = MeasureSpec.getSize(heightMeasureSpec)
        val ratio = aspectRatioHeight / aspectRatioWidth.toFloat()

        val finalWidth: Int
        val finalHeight: Int

        if (aspectRatioFixedWidth) {
            finalWidth = originalWidth
            finalHeight = (originalWidth * ratio).roundToInt()
        } else {
            finalWidth = (originalHeight / ratio).roundToInt()
            finalHeight = originalHeight
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY))
    }


    fun updateLayoutParams(layoutParams: ViewGroup.LayoutParams?, scale: Float) {
        if (layoutParams == null) {
            return
        }

        when (layoutParams) {
            is RelativeLayout.LayoutParams -> {
                layoutParams.setMargins((layoutParams.leftMargin * scale).toInt(),
                    (layoutParams.topMargin * scale).toInt(),
                    (layoutParams.rightMargin * scale).toInt(), (layoutParams.bottomMargin * scale).toInt())
            }
            is LinearLayout.LayoutParams -> {
                layoutParams.setMargins((layoutParams.leftMargin * scale).toInt(),
                    (layoutParams.topMargin * scale).toInt(),
                    (layoutParams.rightMargin * scale).toInt(), (layoutParams.bottomMargin * scale).toInt())
            }
            is FrameLayout.LayoutParams -> {
                layoutParams.setMargins((layoutParams.leftMargin * scale).toInt(),
                    (layoutParams.topMargin * scale).toInt(),
                    (layoutParams.rightMargin * scale).toInt(), (layoutParams.bottomMargin * scale).toInt())
            }
        }

        val lH = layoutParams.height
        val lW = layoutParams.width
        if (lH > 0) {
            layoutParams.height = (lH * scale).toInt()
        }
        if (lW > 0) {
            layoutParams.width = (lW * scale).toInt()
        }
    }
}

package com.affirm.android.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.affirm.android.CardBrand
import com.affirm.android.CardBrand.Companion.fromCardNumber
import com.affirm.android.R
import com.affirm.android.model.CardDetails
import kotlinx.android.synthetic.main.vcn_card_mastercard.view.*
import kotlinx.android.synthetic.main.vcn_card_visa.view.*
import kotlinx.android.synthetic.main.vcn_card_visa_back.view.*
import kotlinx.android.synthetic.main.view_vcn_card.view.*
import kotlin.properties.Delegates

class VCNCardView(context: Context, attrs: AttributeSet) :
    FixedAspectRatioFrameLayout(context, attrs) {

    private var scaled = false
    private var oldElevation: Float = 0.0f

    var vcn by Delegates.observable<CardDetails?>(null) { _, _, new ->
        new?.let { afterSetVcn(it) }
    }

    private fun afterSetVcn(card: CardDetails) {
        val cardNumber = card.number() ?: return

        when (fromCardNumber(cardNumber)) {
            CardBrand.Visa -> {
                val visaFrontView = inflate(context, R.layout.vcn_card_visa, null)
                val visaBackView = inflate(context, R.layout.vcn_card_visa_back, null)

                cardViewInVCNCardView.addView(visaBackView, 0)
                cardViewInVCNCardView.addView(visaFrontView, 1)
                visaFrontView.vcnDisplayCardCvvVisa.text = card.cvv()
                card.expiration()?.let {
                    visaFrontView.vcnDisplayCardExpirationVisa.text = String.format("%s/%s", it.substring(0, 2), it.substring(2, it.length))
                }
                visaFrontView.vcnDisplayCardNumberVisa.text = cardNumber.replace(Regex(".{4}(?!\$)"), "\$0 ")
                visaBackView.authorizedCardholderVisaBack.text = context.getString(R.string.authorized_cardholder, card.cardholderName())
                setUpFlipAnimation(visaFrontView, visaBackView)
            }
            CardBrand.MasterCard -> {
                cardViewInVCNCardView.background = ContextCompat.getDrawable(context, R.drawable.vcn_bg_card)
                val mastercardView = inflate(context, R.layout.vcn_card_mastercard, null)
                cardViewInVCNCardView.addView(mastercardView, 0)

                vcnDisplayCardNumberMastercard.text = cardNumber.replace(Regex(".{4}(?!\$)"), "\$0 ")
                vcnDisplayCardCvvMastercard.text = card.cvv()
                vcnDisplayCardExpirationMastercard.text = card.expiration()
            }
        }
    }

    private fun setUpFlipAnimation(visaFrontView: View, visaBackView: View) {
        val distance = 100000
        val scale = resources.displayMetrics.density * distance
        visaFrontView.cameraDistance = scale
        visaBackView.cameraDistance = scale

        val clearShadowOnStartListener = object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {
                oldElevation = cardViewInVCNCardView.cardElevation
                cardViewInVCNCardView.cardElevation = 0f
            }

            override fun onAnimationCancel(animation: Animator?) {
            }
        }

        val returnShadowOnEndListener = object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                cardViewInVCNCardView.cardElevation = oldElevation
            }

            override fun onAnimationStart(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }
        }

        val inAnimatorFront = createFlipInAnimator(visaFrontView)
        inAnimatorFront.addListener(returnShadowOnEndListener)

        val inAnimatorBack = createFlipInAnimator(visaBackView)
        inAnimatorBack.addListener(returnShadowOnEndListener)

        val outAnimatorFront = createFlipOutAnimator(visaFrontView)
        outAnimatorFront.addListener(clearShadowOnStartListener)

        val outAnimatorBack = createFlipOutAnimator(visaBackView)
        outAnimatorBack.addListener(clearShadowOnStartListener)

        visaFrontView.informationButton.setOnClickListener {
            outAnimatorFront.start()
            inAnimatorBack.start()
        }

        visaBackView.backButton.setOnClickListener {
            outAnimatorBack.start()
            inAnimatorFront.start()
        }
    }

    private fun createFlipInAnimator(view: View): Animator {
        return AnimatorSet().apply {
            val alphaAnimDisappear = ObjectAnimator.ofFloat(view, View.ALPHA, 1F, 0F).setDuration(0)
            play(alphaAnimDisappear)
            val rotationAnim = ObjectAnimator.ofFloat(view, View.ROTATION_Y, -180F, 0F).setDuration(FLIP_ANIM_DURATION)
            rotationAnim.repeatMode = ValueAnimator.REVERSE
            play(rotationAnim)
            val alphaAnimAppear = ObjectAnimator.ofFloat(view, View.ALPHA, 0F, 1F).setDuration(1)
            alphaAnimAppear.startDelay = FLIP_ANIM_DURATION / 2
            play(alphaAnimAppear)
        }
    }

    private fun createFlipOutAnimator(view: View): Animator {
        return AnimatorSet().apply {
            val rotationAnim = ObjectAnimator.ofFloat(view, View.ROTATION_Y, 0F, 180F).setDuration(FLIP_ANIM_DURATION)
            play(rotationAnim)
            val alphaAnimDisappear = ObjectAnimator.ofFloat(view, View.ALPHA, 1F, 0F).setDuration(0)
            alphaAnimDisappear.startDelay = FLIP_ANIM_DURATION / 2
            play(alphaAnimDisappear)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!scaled) {
            scaled = true
            val desiredWith = resources.displayMetrics.density * aspectRatioWidth
            val realWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
            val scale = realWidth / desiredWith
            updateChildLayoutParams(this, scale)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun updateChildLayoutParams(viewGroup: ViewGroup, scale: Float) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)

            when (child) {
                is TextView -> child.setTextSize(TypedValue.COMPLEX_UNIT_PX, child.textSize * scale)
                is CardView -> {
                    child.radius *= scale
                    updateChildLayoutParams(child, scale)
                }
                is ViewGroup -> updateChildLayoutParams(child, scale)
            }

            child.setPadding((child.paddingLeft * scale).toInt(), (child.paddingTop * scale).toInt(),
                (child.paddingRight * scale).toInt(), (child.paddingBottom * scale).toInt())
            updateLayoutParams(child.layoutParams, scale)
        }
    }

    companion object {
        const val FLIP_ANIM_DURATION = (1000 * 1.0).toLong()
    }
}

package com.affirm.android.widget

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.affirm.android.CardExpirationUtils
import com.affirm.android.R
import java.util.concurrent.TimeUnit

class CountDownTimerView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    companion object {
        const val EXPIRATION_TIME = 24 * 60 * 60 * 1000L
    }

    private var currentMillis: Long? = null

    private val countDownTimer by lazy {
        val cardExpiredTimeMillis = CardExpirationUtils
            .cardExpiredTimeMillis(context.applicationContext)
        val time = if (cardExpiredTimeMillis > 0) {
            cardExpiredTimeMillis
        } else {
            EXPIRATION_TIME
        }
        object : CountDownTimer(time, 1000) {
            override fun onTick(millis: Long) {
                currentMillis = millis
                val validTime = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis)
                        - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
                )
                text = resources.getString(R.string.card_valid_expiration, validTime)
            }

            override fun onFinish() {
                currentMillis = 0
                text = resources.getString(R.string.card_valid_expiration, "00:00:00")
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        countDownTimer.start()
    }

    override fun onDetachedFromWindow() {
        currentMillis?.let {
            CardExpirationUtils.saveCardExpiredTime(context.applicationContext,
                it, System.currentTimeMillis())
        }
        countDownTimer.cancel()
        super.onDetachedFromWindow()
    }
}

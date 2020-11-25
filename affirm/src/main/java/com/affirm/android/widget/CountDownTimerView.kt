package com.affirm.android.widget

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.affirm.android.R
import java.util.*
import java.util.concurrent.TimeUnit

class CountDownTimerView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    private var countDownTimer: CountDownTimer? = null

    fun start(expirationDate: Date?) {
        if (expirationDate != null) {
            val millisInFuture = expirationDate.time - System.currentTimeMillis()
            countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
                override fun onTick(millis: Long) {
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
                    text = resources.getString(R.string.card_valid_expiration, "00:00:00")
                }
            }.apply {
                start()
            }
        } else {
            text = resources.getString(R.string.card_valid_expiration, "00:00:00")
        }
    }

    override fun onDetachedFromWindow() {
        countDownTimer?.cancel()
        super.onDetachedFromWindow()
    }
}

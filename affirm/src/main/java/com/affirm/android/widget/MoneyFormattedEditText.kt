package com.affirm.android.widget

import android.content.Context
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import com.affirm.android.MoneyTextParser
import com.affirm.android.R
import com.affirm.android.TextParser
import org.joda.money.Money

private const val VALID_CHARS = "0123456789$..,"

class MoneyFormattedEditText(context: Context, attrs: AttributeSet?) : FormattedEditText<Money>(context, attrs) {

    private lateinit var parser: MoneyTextParser
    override val textParser: TextParser<Money>
        get() = parser

    init {
        setHint(R.string.zero_whole_dollar_hint)
        inputType = InputType.TYPE_CLASS_NUMBER
        keyListener = DigitsKeyListener.getInstance(VALID_CHARS)

        if (!isInEditMode) {
            parser = MoneyTextParser()
            addTextChangedListener(parser)
        }
    }
}

package com.affirm.android.widget

import android.content.Context
import android.util.AttributeSet
import com.affirm.android.R
import com.affirm.android.TextParser
import com.google.android.material.textfield.TextInputEditText

abstract class FormattedEditText<T>
@JvmOverloads
protected constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = R.attr.editTextStyle) :
    TextInputEditText(context, attrs, defStyleAttr) {

    protected abstract val textParser: TextParser<T>

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (isInEditMode) return

        addTextChangedListener(textParser)
    }

    val raw: T
        get() {
            return textParser.getRaw(textParser.getRawText(text))
        }
}

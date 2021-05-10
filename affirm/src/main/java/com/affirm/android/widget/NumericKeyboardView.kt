package com.affirm.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.gridlayout.widget.GridLayout
import com.affirm.android.R

class NumericKeyboardView(context: Context?, attrs: AttributeSet?) : GridLayout(context, attrs) {

    private val clickListener = OnClickListener()

    fun setTarget(editText: EditText?) {
        clickListener.editText = editText
    }

    class OnClickListener : View.OnClickListener {
        var editText: EditText? = null

        override fun onClick(v: View) {
            val editText = editText ?: return
            val character = if (v is TextView) v.text else null
            val keyId = v.id
            val editable = editText.text
            val start = editText.selectionStart
            if (keyId == R.id.backspace) {
                if (start > 0) {
                    editable.delete(start - 1, start)
                }
            } else {
                editable.insert(start, character)
            }
        }
    }

    init {
        rowCount = 4
        columnCount = 3
        View.inflate(context, R.layout.keyboard_layout, this)
        for (keyId in intArrayOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5,
            R.id.button6, R.id.button7, R.id.button8, R.id.button9, R.id.backspace
        )) {
            findViewById<View>(keyId).setOnClickListener(clickListener)
        }
    }
}
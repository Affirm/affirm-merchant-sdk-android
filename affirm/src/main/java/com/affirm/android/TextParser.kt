package com.affirm.android

import android.text.TextWatcher

interface TextParser<T> : TextWatcher {
    fun getRaw(input: CharSequence?): T
    fun getRawText(input: CharSequence?): CharSequence?
}
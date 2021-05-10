package com.affirm.android

import android.text.Editable
import android.text.Selection
import org.joda.money.CurrencyUnit
import org.joda.money.Money

class MoneyTextParser : TextParser<Money> {
    private var isChanging = false
    private var lastValidInput = ""

    // Defaults to USD
    private val currencyUnit: CurrencyUnit = CurrencyUnit.USD

    /**
     * Returns money amount of input, interpreting the amount as the lowest possible currency unit
     * returns null for invalid inputs or inputs that exceed the Integer.MAX_VALUE
     *
     * @param input to parse
     * @return money amount or null if invalid input
     */
    override fun getRaw(input: CharSequence?): Money {
        val money = getMoneyFromString(input)
        return money ?: fromInputAmount(0)
    }

    private fun getMoneyFromString(input: CharSequence?): Money? {
        return try {
            var rawInput = getRawText(input).toString()
            if (rawInput.isEmpty()) {
                rawInput = "0"
            }
            val inputAmount = rawInput.toInt()
            fromInputAmount(inputAmount)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun fromInputAmount(inputAmount: Int): Money {
        return Money.ofMajor(currencyUnit, inputAmount.toLong())
    }

    private fun getReplacementString(input: CharSequence): String {
        val moneyVal = getRaw(input)
        return when {
            getRawText(input).isNullOrEmpty() -> {
                ""
            }
            getMoneyFromString(input) == null -> {
                lastValidInput
            }
            else -> {
                moneyVal.format(false).replace("\\.\\d+".toRegex(), "")
            }
        }
    }

    override fun getRawText(input: CharSequence?): CharSequence? {
        var raw = input
        if (input != null) {
            raw = input.toString().replace("\\.\\d+".toRegex(), "")
        }
        return stripNonDigits(raw)
    }

    private fun stripNonDigits(input: CharSequence?, maxLength: Int = -1): String {
        val digitsBuilder = StringBuilder()
        if (input != null) {
            val inputLength = input.length
            var numDigits = 0
            var count = 0
            while (count < inputLength && (maxLength < 0 || numDigits < maxLength)) {
                val c = input[count]
                if (Character.isDigit(c)) {
                    numDigits++
                    digitsBuilder.append(c)
                }
                count++
            }
        }
        return digitsBuilder.toString()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun afterTextChanged(editable: Editable) {
        if (isChanging) {
            return
        }
        isChanging = true
        val replacementString = getReplacementString(editable)
        editable.replace(0, editable.length, replacementString)
        lastValidInput = replacementString
        Selection.setSelection(editable, editable.length)
        isChanging = false
    }
}
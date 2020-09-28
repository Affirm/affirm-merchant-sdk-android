package com.affirm.android

import org.joda.money.BigMoneyProvider
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.joda.money.format.MoneyAmountStyle
import org.joda.money.format.MoneyFormatterBuilder
import java.util.*

val DEFAULT_CURRENCY: CurrencyUnit = CurrencyUnit.USD

fun Int.fromCentsToMoney(
    currency: CurrencyUnit = DEFAULT_CURRENCY): Money = Money.ofMinor(
    currency, this.toLong())

fun Long.fromCentsToMoney(
    currency: CurrencyUnit = DEFAULT_CURRENCY): Money = Money.ofMinor(
    currency, this)

fun BigMoneyProvider.format(omitCents: Boolean = false): String {
    val stringBuilder = StringBuilder()
    val moneyAmountStyle = MoneyAmountStyle.of(Locale.getDefault())
    val bigMoney = this.toBigMoney()
    val moneyFormatter = MoneyFormatterBuilder().appendCurrencySymbolLocalized()
        .appendAmountLocalized()
        .toFormatter(Locale.getDefault())
    if (bigMoney.isNegative) {
        stringBuilder.append(moneyAmountStyle.negativeSignCharacter)
    }
    stringBuilder.append(moneyFormatter.print(bigMoney.abs()))
    if (omitCents && stringBuilder.length > 3) {
        stringBuilder.setLength(stringBuilder.length - 3)
    }
    return stringBuilder.toString()
}
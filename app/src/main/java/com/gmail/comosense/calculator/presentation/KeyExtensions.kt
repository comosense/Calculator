package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.R
import java.text.DecimalFormatSymbols
import java.util.Locale

fun Key.displayText(locale: Locale): String? = when (this) {
    is SymbolKey.Point
        -> DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()

    is SymbolKey
        -> symbol.text

    is CommandKey.Clear
        -> null

    is CommandKey.Delete
        -> null

    is CommandKey.Equals
        -> "="

    is ActionKey.OperatorBox
        -> null
}

fun Key.drawable(): Int? = when (this) {
    is CommandKey.Clear
        -> R.drawable.ic_cancel

    is CommandKey.Delete
        -> R.drawable.ic_backspace

    is ActionKey.OperatorBox
        -> R.drawable.ic_operatorkeybox

    else
        -> null
}
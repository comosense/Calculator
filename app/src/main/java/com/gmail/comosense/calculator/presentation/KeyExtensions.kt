package com.gmail.comosense.calculator.presentation

import java.text.DecimalFormatSymbols
import java.util.Locale

fun Key.displayText(locale: Locale): String = when (this) {
    is SymbolKey.Point
        -> DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()

    is SymbolKey
        -> symbol.text

    is CommandKey.Clear
        -> "C"

    is CommandKey.Delete
        -> "←"

    is CommandKey.Equals
        -> "="
}

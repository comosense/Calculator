package com.gmail.comosense.calculator.presentation

import java.text.DecimalFormatSymbols
import java.util.Locale

fun Key.displayText(locale: Locale): String = when (this) {
    SymbolKey.Point -> {
        DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
    }

    else -> {
        text
    }
}

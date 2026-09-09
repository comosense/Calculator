package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.formatNumericString
import java.util.Locale

fun List<Symbol>.formatSymbols(locale: Locale): String {
    return buildString {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric() {
            if (numericBuffer.isEmpty()) return

            append(
                formatNumericString(
                    value = numericBuffer.toString(),
                    locale = locale,
                )
            )
            numericBuffer.clear()
        }

        for (symbol in this@formatSymbols) {
            when (symbol) {
                is Symbol.Numeric
                    -> numericBuffer.append(symbol.text)

                else -> {
                    flushNumeric()
                    append(symbol.text)
                }
            }
        }
        flushNumeric()
    }
}

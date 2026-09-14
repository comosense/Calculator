package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.formatNumericString
import com.gmail.comosense.calculator.domain.Symbol
import java.util.Locale

val Symbol.toString: String
    get() = when (this) {
        is Symbol.Sign.Positive
            -> "+"

        is Symbol.Sign.Negative
            -> "-"

        is Symbol.Numeric.Digit
            -> value.toString()

        is Symbol.Numeric.Point
            -> "."

        is Symbol.FactorStart.OpenParenthesis
            -> "("

        is Symbol.FactorEnd.CloseParenthesis
            -> ")"

        is Symbol.Operator.Add
            -> "+"

        is Symbol.Operator.Subtract
            -> "-"

        is Symbol.Operator.Multiply
            -> "×"

        is Symbol.Operator.Divide
            -> "÷"

        is Symbol.Error
            -> value
    }

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
                    -> numericBuffer.append(symbol.toString)

                else -> {
                    flushNumeric()
                    append(symbol.toString)
                }
            }
        }
        flushNumeric()
    }
}

package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

val Symbol.text: String
    get() = when (this) {
        is Symbol.Sign.Positive -> "+"
        is Symbol.Sign.Negative -> "-"
        is Symbol.Numeric.Digit -> value.toString()
        is Symbol.Numeric.Point -> "."
        is Symbol.FactorStart.OpenParenthesis -> "("
        is Symbol.FactorEnd.CloseParenthesis -> ")"
        is Symbol.Operator.Add -> "+"
        is Symbol.Operator.Subtract -> "-"
        is Symbol.Operator.Multiply -> "×"
        is Symbol.Operator.Divide -> "÷"
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
                is Symbol.Numeric ->
                    numericBuffer.append(symbol.text)

                else -> {
                    flushNumeric()
                    append(symbol.text)
                }
            }
        }
        flushNumeric()
    }
}

private fun formatNumericString(value: String, locale: Locale): String {
    val pointIndex: Int = value.indexOf(".")
    val integerPart: String = if (pointIndex >= 0) {
        value.substring(0, pointIndex)
    } else {
        value
    }

    val decimalPart: String? = if (pointIndex >= 0) {
        value.substring(pointIndex + 1)
    } else {
        null
    }

    val formattedIntegerPart: String = formatIntegerPart(
        integerPart = integerPart,
        locale = locale,
    )

    return if (decimalPart == null) {
        formattedIntegerPart
    } else {
        buildString {
            append(formattedIntegerPart)
            append(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
            append(decimalPart)
        }
    }
}

private fun formatIntegerPart(integerPart: String, locale: Locale): String {
    if (integerPart.isEmpty()) return integerPart

    val formatter: NumberFormat = NumberFormat.getNumberInstance(locale)
    if (formatter !is DecimalFormat) {
        return integerPart
    }

    formatter.isGroupingUsed = true
    formatter.maximumIntegerDigits = integerPart.length
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0

    return formatter.format(BigInteger(integerPart))
}

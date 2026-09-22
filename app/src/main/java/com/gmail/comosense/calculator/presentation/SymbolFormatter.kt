package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

fun decimalSeparator(locale: Locale): Char {
    return DecimalFormatSymbols.getInstance(locale).decimalSeparator
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
                is Symbol.Numeric.Digit ->
                    numericBuffer.append(symbol.value)

                is Symbol.Numeric.Point ->
                    numericBuffer.append(".")

                else -> {
                    flushNumeric()

                    when (symbol) {
                        Symbol.Sign.Positive ->
                            append("+")

                        Symbol.Sign.Negative ->
                            append("-")

                        Symbol.FactorStart.OpeningParenthesis ->
                            append("(")

                        Symbol.FactorEnd.ClosingParenthesis ->
                            append(")")

                        Symbol.Operator.Add ->
                            append("+")

                        Symbol.Operator.Subtract ->
                            append("-")

                        Symbol.Operator.Multiply ->
                            append("×")

                        Symbol.Operator.Divide ->
                            append("÷")
                    }
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
            append(decimalSeparator(locale))
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

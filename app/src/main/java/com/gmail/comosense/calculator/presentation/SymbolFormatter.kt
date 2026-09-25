package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class SymbolFormatter(locale: Locale) {
    private val integerFormatter: DecimalFormat? =
        (NumberFormat.getNumberInstance(locale) as? DecimalFormat)?.apply {
            isGroupingUsed = true
            maximumIntegerDigits = Int.MAX_VALUE
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }

    val decimalSeparator: Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    fun format(symbols: List<Symbol>): String {
        return buildString {
            val numericBuffer: StringBuilder = StringBuilder()

            fun flushNumeric() {
                if (numericBuffer.isEmpty()) return

                append(formatNumericString(numericBuffer.toString()))
                numericBuffer.clear()
            }

            for (symbol in symbols) {
                when (symbol) {
                    is Symbol.Numeric -> {
                        numericBuffer.append(symbol.toCanonicalizedText)
                    }

                    else -> {
                        flushNumeric()
                        append(symbol.toCanonicalizedText)
                    }
                }
            }
            flushNumeric()
        }
    }

    private fun formatNumericString(value: String): String {
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
        val formattedIntegerPart: String = formatIntegerPart(integerPart)

        return if (decimalPart == null) {
            formattedIntegerPart
        } else {
            buildString {
                append(formattedIntegerPart)
                append(decimalSeparator)
                append(decimalPart)
            }
        }
    }

    private fun formatIntegerPart(integerPart: String): String {
        if (integerPart.isEmpty() || integerFormatter == null) return integerPart

        return integerFormatter.format(BigInteger(integerPart))
    }

    private val Symbol.toCanonicalizedText: String
        get() = when (this) {
            is Symbol.Sign.Positive -> "+"
            is Symbol.Sign.Negative -> "-"
            is Symbol.Numeric.Digit -> value.toString()
            is Symbol.Numeric.Point -> "."
            is Symbol.FactorStart.OpeningParenthesis -> "("
            is Symbol.FactorEnd.ClosingParenthesis -> ")"
            is Symbol.Operator.Add -> "+"
            is Symbol.Operator.Subtract -> "-"
            is Symbol.Operator.Multiply -> "×"
            is Symbol.Operator.Divide -> "÷"
        }
}

package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class SymbolFormatter(
    locale: Locale,
) {
    companion object {
        private const val INTERNAL_DECIMAL_SEPARATOR: Char = '.'
    }

    private val numberLocale: Locale =
        runCatching {
            Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("nu", "latn")
                .build()
        }.getOrDefault(locale)
    private val integerFormatter: DecimalFormat? =
        (NumberFormat.getNumberInstance(numberLocale) as? DecimalFormat)?.apply {
            decimalFormatSymbols = decimalFormatSymbols.apply {
                zeroDigit = '0'
            }
            isGroupingUsed = true
            maximumIntegerDigits = Int.MAX_VALUE
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    private val decimalSeparator: Char =
        DecimalFormatSymbols
            .getInstance(numberLocale)
            .decimalSeparator

    fun text(symbol: Symbol): String =
        when (symbol) {
            is Symbol.Sign.Positive -> "+"
            is Symbol.Sign.Negative -> "-"
            is Symbol.Numeric.Digit -> symbol.value.toString()
            is Symbol.Numeric.Point -> decimalSeparator.toString()
            is Symbol.Constant.Euler -> "e"
            is Symbol.Constant.Pi -> "π"
            is Symbol.Factor.Function.Sqrt -> "√"
            is Symbol.FactorStart.OpeningParenthesis -> "("
            is Symbol.FactorStart.Function.Exp -> "exp"
            is Symbol.FactorStart.Function.Ln -> "ln"
            is Symbol.FactorStart.Function.Log -> "log"
            is Symbol.FactorStart.Function.Sin -> "sin"
            is Symbol.FactorStart.Function.Cos -> "cos"
            is Symbol.FactorStart.Function.Tan -> "tan"
            is Symbol.FactorStart.Function.Arcsin -> "asin"
            is Symbol.FactorStart.Function.Arccos -> "acos"
            is Symbol.FactorStart.Function.Arctan -> "atan"
            is Symbol.FactorEnd.ClosingParenthesis -> ")"
            is Symbol.Operator.Add -> "+"
            is Symbol.Operator.Subtract -> "-"
            is Symbol.Operator.Multiply -> "×"
            is Symbol.Operator.Divide -> "÷"
            is Symbol.Operator.Power -> "^"
            is Symbol.PostfixOperator.Factorial -> "!"
        }

    fun format(
        symbols: List<Symbol>,
    ): String {
        return buildString {
            val numericBuffer: StringBuilder = StringBuilder()

            fun flushNumeric() {
                if (numericBuffer.isEmpty()) {
                    return
                }

                append(formatNumericString(numericBuffer.toString()))
                numericBuffer.clear()
            }

            for (symbol in symbols) {
                when (symbol) {
                    is Symbol.Numeric.Digit ->
                        numericBuffer.append(symbol.value)

                    is Symbol.Numeric.Point ->
                        numericBuffer.append(INTERNAL_DECIMAL_SEPARATOR)

                    else -> {
                        flushNumeric()
                        append(symbol.toFormatText())
                    }
                }
            }
            flushNumeric()
        }
    }

    private fun Symbol.toFormatText(): String =
        when (this) {
            is Symbol.FactorStart.Function ->
                text(this) + text(Symbol.FactorStart.OpeningParenthesis)

            else ->
                text(this)
        }

    private fun formatNumericString(
        value: String,
    ): String {
        val pointIndex: Int = value.indexOf(INTERNAL_DECIMAL_SEPARATOR)

        return if (pointIndex >= 0) {
            buildString {
                append(formatIntegerString(value.substring(0, pointIndex)))
                append(decimalSeparator)
                append(value.substring(pointIndex + 1))
            }
        } else {
            formatIntegerString(value)
        }
    }

    private fun formatIntegerString(
        integerString: String,
    ): String {
        if (integerString.isEmpty() || integerFormatter == null) {
            return integerString
        }

        val firstNonZeroIndex: Int =
            integerString.indexOfFirst { c -> c != '0' }

        if (firstNonZeroIndex < 0) {
            return integerString
        }

        val leadingZeros: String =
            integerString.substring(0, firstNonZeroIndex)
        val significantDigits: String =
            integerString.substring(firstNonZeroIndex)

        val formattedDigits: String =
            significantDigits.toBigIntegerOrNull()?.let { integerValue ->
                integerFormatter.format(integerValue)
            } ?: significantDigits

        return leadingZeros + formattedDigits
    }
}

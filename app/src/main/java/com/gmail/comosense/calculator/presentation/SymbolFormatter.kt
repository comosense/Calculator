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

    private val integerFormatter: DecimalFormat? =
        (NumberFormat.getNumberInstance(locale) as? DecimalFormat)?.apply {
            isGroupingUsed = true
            maximumIntegerDigits = Int.MAX_VALUE
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    private val decimalSeparator: Char =
        DecimalFormatSymbols
            .getInstance(locale)
            .decimalSeparator
    private val Symbol.expressionText: String
        get() = when (this) {
            is Symbol.FactorStart ->
                text(this) + text(Symbol.FactorStart.OpeningParenthesis)

            else ->
                text(this)
        }

    fun text(symbol: Symbol): String = when (symbol) {
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
                        append(symbol.expressionText)
                    }
                }
            }
            flushNumeric()
        }
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

        return integerString.toBigIntegerOrNull()?.let { integerValue ->
            integerFormatter.format(integerValue)
        } ?: integerString
    }
}

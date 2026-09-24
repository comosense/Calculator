package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class SymbolFormatter(locale: Locale) {
    val decimalSeparator: Char = DecimalFormatSymbols.getInstance(locale).decimalSeparator

    private val integerFormatter: DecimalFormat? =
        (NumberFormat.getNumberInstance(locale) as? DecimalFormat)?.apply {
            isGroupingUsed = true
            maximumIntegerDigits = Int.MAX_VALUE
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }

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
}

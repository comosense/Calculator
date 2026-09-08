package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.CalculatorError
import com.gmail.comosense.calculator.domain.Token
import com.gmail.comosense.calculator.domain.calculate

class CalculatorService(
    private val precision: Int = 50,
    private val displayScale: Int = 20,
) {
    fun calculate(expression: List<Symbol>): List<Symbol> {
        return when (val r: Result<List<Token>, SymbolParserError> = parseTokens(expression)) {
            is Result.Ok -> {
                when (val r =
                    calculate(r.value, precision).toDisplaySymbols(displayScale)) {
                    is Result.Ok -> r.value
                    is Result.Err -> listOf(Symbol.Error(errorMessage(r.error)))
                }
            }

            is Result.Err -> {
                listOf(Symbol.Error(errorMessage(r.error)))
            }
        }
    }

    private fun errorMessage(error: ResultConverterError): String = when (error) {
        is ResultConverterError.UnsupportedCharacter
            -> "Invalid result"

        is ResultConverterError.Calculation -> when (error.error) {
            CalculatorError.InvalidExpression
                -> "Invalid expression"

            CalculatorError.DivisionByZero
                -> "Division by zero"
        }
    }

    private fun errorMessage(error: SymbolParserError): String = when (error) {
        is SymbolParserError.UnsupportedSymbol,
        is SymbolParserError.IllegalNumeric
            -> "Invalid expression"
    }
}
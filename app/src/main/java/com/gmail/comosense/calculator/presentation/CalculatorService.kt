package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.CalculatorError
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.domain.SymbolParserError
import com.gmail.comosense.calculator.domain.Token
import com.gmail.comosense.calculator.domain.calculate
import com.gmail.comosense.calculator.domain.parseTokens

enum class CalculatorServiceError {
    InvalidResult,
    InvalidExpression,
    DivisionByZero,
    Arithmetic,
}

class CalculatorService(
    private val precision: Int = 50,
    private val displayScale: Int = 20,
) {
    fun calculate(expression: List<Symbol>): Result<List<Symbol>, CalculatorServiceError> {
        return when (val r: Result<List<Token>, SymbolParserError> =
            parseTokens(expression)) {
            is Result.Ok -> {
                when (val res: Result<List<Symbol>, ResultConverterError> =
                    calculate(r.value, precision).toDisplaySymbols(displayScale)) {
                    is Result.Ok -> Result.Ok(res.value)
                    is Result.Err -> Result.Err(error(res.error))
                }
            }

            is Result.Err -> {
                Result.Err(error(r.error))
            }
        }
    }

    private fun error(error: ResultConverterError): CalculatorServiceError = when (error) {
        is ResultConverterError.UnsupportedCharacter ->
            CalculatorServiceError.InvalidResult

        is ResultConverterError.Calculation -> when (error.error) {
            CalculatorError.InvalidExpression ->
                CalculatorServiceError.InvalidExpression

            CalculatorError.DivisionByZero ->
                CalculatorServiceError.DivisionByZero

            CalculatorError.Arithmetic ->
                CalculatorServiceError.Arithmetic
        }
    }

    private fun error(error: SymbolParserError): CalculatorServiceError = when (error) {
        is SymbolParserError.UnsupportedSymbol,
        is SymbolParserError.IllegalNumeric ->
            CalculatorServiceError.InvalidExpression
    }
}

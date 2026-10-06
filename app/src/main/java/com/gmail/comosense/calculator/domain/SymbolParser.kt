package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal

sealed interface SymbolParserError {
    data object IllegalNumeric : SymbolParserError
    data object UnsupportedSymbol : SymbolParserError
}

fun parseTokens(symbols: List<Symbol>): Result<List<Token>, SymbolParserError> {
    val tokens: List<Token> = buildList {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric(): Boolean {
            if (numericBuffer.isEmpty()) return true

            val numericString: String = numericBuffer.toString()
            numericBuffer.clear()

            return try {
                add(Token.Numeric(BigDecimal(numericString)))
                true
            } catch (_: NumberFormatException) {
                false
            }
        }

        for (symbol in symbols) {
            when (symbol) {
                is Symbol.Numeric.Digit ->
                    numericBuffer.append(symbol.value)

                is Symbol.Numeric.Point ->
                    numericBuffer.append(".")

                else -> {
                    if (!flushNumeric()) return Result.Err(SymbolParserError.IllegalNumeric)

                    when (val tokenResult: Result<Token, SymbolParserError> = symbol.toToken()) {
                        is Result.Ok -> add(tokenResult.value)
                        is Result.Err -> return tokenResult
                    }
                }
            }
        }

        if (!flushNumeric()) return Result.Err(SymbolParserError.IllegalNumeric)
    }

    return Result.Ok(tokens)
}

fun Symbol.toToken(): Result<Token, SymbolParserError> = when (this) {
    is Symbol.Sign.Positive ->
        Result.Ok(Token.Sign.Positive)

    is Symbol.Sign.Negative ->
        Result.Ok(Token.Sign.Negative)

    is Symbol.Constant.Pi ->
        Result.Ok(Token.Constant.Pi)

    is Symbol.Constant.Euler ->
        Result.Ok(Token.Constant.Euler)

    is Symbol.Factor.Function.Sqrt ->
        Result.Ok(Token.Factor.Function.Sqrt)

    is Symbol.FactorStart.OpeningParenthesis ->
        Result.Ok(Token.FactorStart.OpeningParenthesis)

    is Symbol.FactorStart.Function.Sin ->
        Result.Ok(Token.FactorStart.Function.Sin)

    is Symbol.FactorStart.Function.Cos ->
        Result.Ok(Token.FactorStart.Function.Cos)

    is Symbol.FactorStart.Function.Tan ->
        Result.Ok(Token.FactorStart.Function.Tan)

    is Symbol.FactorStart.Function.Log ->
        Result.Ok(Token.FactorStart.Function.Log)

    is Symbol.FactorStart.Function.Ln ->
        Result.Ok(Token.FactorStart.Function.Ln)

    is Symbol.FactorEnd.ClosingParenthesis ->
        Result.Ok(Token.FactorEnd.ClosingParenthesis)

    is Symbol.Operator.Add ->
        Result.Ok(Token.Operator.Add)

    is Symbol.Operator.Subtract ->
        Result.Ok(Token.Operator.Subtract)

    is Symbol.Operator.Multiply ->
        Result.Ok(Token.Operator.Multiply)

    is Symbol.Operator.Divide ->
        Result.Ok(Token.Operator.Divide)

    is Symbol.SpecialOperator.Power ->
        Result.Ok(Token.SpecialOperator.Power)

    is Symbol.SpecialOperator.Factorial ->
        Result.Ok(Token.SpecialOperator.Factorial)

    else ->
        Result.Err(SymbolParserError.UnsupportedSymbol)
}

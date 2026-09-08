package com.gmail.comosense.calculator.presentation

import android.util.Log
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.Token
import java.math.BigDecimal

sealed interface SymbolParserError {
    data object UnsupportedSymbol : SymbolParserError
    data object IllegalNumeric : SymbolParserError
}

fun parseTokens(symbols: List<Symbol>): Result<List<Token>, SymbolParserError> {
    val tokens: List<Token> = buildList {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric(): Result<Unit, SymbolParserError> {
            if (numericBuffer.isEmpty()) return Result.Ok(Unit)

            val numericString = numericBuffer.toString()
            numericBuffer.clear()

            return try {
                add(Token.Numeric(BigDecimal(numericString)))
                Result.Ok(Unit)
            } catch (e: NumberFormatException) {
                Log.e(
                    "SymbolParser",
                    "[parseTokens::flushNumeric] invalid numeric: ${e.toString()}"
                )
                Result.Err(SymbolParserError.IllegalNumeric)
            }
        }

        for (symbol in symbols) {
            when (symbol) {
                is Symbol.Numeric.Digit,
                Symbol.Numeric.Point -> {
                    numericBuffer.append(symbol.text)
                }

                else -> {
                    when (val r: Result<Unit, SymbolParserError> = flushNumeric()) {
                        is Result.Ok -> Unit
                        is Result.Err -> return r
                    }

                    when (val r: Result<Token, SymbolParserError> = symbol.toToken()) {
                        is Result.Ok -> add(r.value)
                        is Result.Err -> return r
                    }
                }
            }
        }

        when (val r: Result<Unit, SymbolParserError> = flushNumeric()) {
            is Result.Ok -> Unit
            is Result.Err -> return r
        }
    }

    return Result.Ok(tokens)
}

private fun Symbol.toToken(): Result<Token, SymbolParserError> = when (this) {
    is Symbol.Sign.Positive ->
        Result.Ok(Token.Sign.Positive)

    is Symbol.Sign.Negative ->
        Result.Ok(Token.Sign.Negative)

    is Symbol.FactorStart.OpenParenthesis ->
        Result.Ok(Token.FactorStart.OpenParenthesis)

    is Symbol.FactorEnd.CloseParenthesis ->
        Result.Ok(Token.FactorEnd.CloseParenthesis)

    is Symbol.Operator.Add ->
        Result.Ok(Token.Operator.Add)

    is Symbol.Operator.Subtract ->
        Result.Ok(Token.Operator.Subtract)

    is Symbol.Operator.Multiply ->
        Result.Ok(Token.Operator.Multiply)

    is Symbol.Operator.Divide ->
        Result.Ok(Token.Operator.Divide)

    else -> {
        Log.e("SymbolConverter", "[Symbol.toToken()]$this")
        Result.Err(SymbolParserError.UnsupportedSymbol)
    }
}

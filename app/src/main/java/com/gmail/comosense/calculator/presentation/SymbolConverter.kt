package com.gmail.comosense.calculator.presentation

import android.util.Log
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.CalculateError
import com.gmail.comosense.calculator.domain.Token
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface SymbolConverterError {
    data object UnsupportedSymbol : SymbolConverterError
    data object IllegalNumeric : SymbolConverterError
    data object UnsupportedCharacter : SymbolConverterError
    data class Calculation(val error: CalculateError) : SymbolConverterError
}

fun tokensFrom(symbols: List<Symbol>): Result<List<Token>, SymbolConverterError> {
    val tokens: List<Token> = buildList {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric(): Result<Unit, SymbolConverterError> {
            if (numericBuffer.isNotEmpty()) {
                val numericString = numericBuffer.toString()
                numericBuffer.clear()
                try {
                    add(Token.Numeric(BigDecimal(numericString)))
                } catch (e: NumberFormatException) {
                    Log.e("SymbolConverter", "[tokensFrom::flushNumeric]${e.toString()}")
                    return Result.Err(SymbolConverterError.IllegalNumeric)
                }
            }
            return Result.Ok(Unit)
        }

        for (symbol in symbols) {
            when (symbol) {
                is Symbol.Numeric.Digit, Symbol.Numeric.Point -> numericBuffer.append(symbol.text)
                else -> {
                    when (val r: Result<Unit, SymbolConverterError> = flushNumeric()) {
                        is Result.Ok -> {}
                        is Result.Err -> return r
                    }
                    when (val r: Result<Token, SymbolConverterError> = symbol.toToken()) {
                        is Result.Ok -> add(r.value)
                        is Result.Err -> {
                            Log.e("SymbolConverter", "[tokensFrom]${r.error}")
                            return r
                        }
                    }
                }
            }
        }
        when (val r: Result<Unit, SymbolConverterError> = flushNumeric()) {
            is Result.Ok -> {}
            is Result.Err -> return r
        }
    }

    return Result.Ok(tokens)
}

fun Result<BigDecimal, CalculateError>.toSymbols(scale: Int): Result<List<Symbol>, SymbolConverterError> {
    fun parse(result: BigDecimal): Result<List<Symbol>, SymbolConverterError> {
        val displayResult = if (result.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            result.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
        }
        val symbols: List<Symbol> = buildList {
            for (c: Char in displayResult.toPlainString()) {
                val symbol = when (c) {
                    in '0'..'9' -> Symbol.Numeric.Digit(c.digitToInt())
                    '.' -> Symbol.Numeric.Point
                    '-' -> Symbol.Sign.Negative
                    else -> {
                        Log.e("SymbolConverter", "[CalculateResult.toSymbols()]$c")
                        return Result.Err(SymbolConverterError.UnsupportedCharacter)
                    }
                }
                add(symbol)
            }
        }
        return Result.Ok(symbols)
    }

    return when (val r: Result<BigDecimal, CalculateError> = this) {
        is Result.Ok
            -> parse(r.value)

        is Result.Err
            -> Result.Err(SymbolConverterError.Calculation(r.error))
    }
}

private fun Symbol.toToken(): Result<Token, SymbolConverterError> = when (this) {
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
        Result.Err(SymbolConverterError.UnsupportedSymbol)
    }
}

package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.CalculatorError
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface ResultConverterError {
    data object UnsupportedCharacter : ResultConverterError
    data class Calculation(val error: CalculatorError) : ResultConverterError
}

fun Result<BigDecimal, CalculatorError>.toDisplaySymbols(scale: Int): Result<List<Symbol>, ResultConverterError> {
    return when (val r: Result<BigDecimal, CalculatorError> = this) {
        is Result.Ok -> r.value.toDisplaySymbol(scale)
        is Result.Err -> Result.Err(ResultConverterError.Calculation(r.error))
    }
}

private fun BigDecimal.toDisplaySymbol(scale: Int): Result<List<Symbol>, ResultConverterError> {
    val displayResult: BigDecimal = if (compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal.ZERO
    } else {
        setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    val symbols: List<Symbol> = buildList {
        for (c in displayResult.toPlainString()) {
            add(
                when (c) {
                    in '0'..'9' -> Symbol.Numeric.Digit(c.digitToInt())
                    '.' -> Symbol.Numeric.Point
                    '-' -> Symbol.Sign.Negative
                    else -> return Result.Err(ResultConverterError.UnsupportedCharacter)
                }
            )
        }
    }
    return Result.Ok(symbols)
}

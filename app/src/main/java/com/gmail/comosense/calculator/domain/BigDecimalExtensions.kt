package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal

internal object BigDecimalExtensionsConstraints {
    const val MAX_RESULT_SIZE: Int = Constraints.MAX_RESULT_SIZE
}

enum class BigDecimalError {
    TooLarge,
    Unsupported,
}

fun BigDecimal.toSymbols(): Result<List<Symbol>, BigDecimalError> {
    val plainString: String = stripTrailingZeros().toPlainString()
    if (plainString.length > BigDecimalExtensionsConstraints.MAX_RESULT_SIZE) {
        return Result.Err(BigDecimalError.TooLarge)
    }

    val symbols: List<Symbol> = buildList {
        for (c in plainString) {
            add(
                when (c) {
                    in '0'..'9' -> Symbol.Numeric.Digit(c.digitToInt())
                    '.' -> Symbol.Numeric.Point
                    '-' -> Symbol.Sign.Negative
                    else -> return Result.Err(BigDecimalError.Unsupported)
                }
            )
        }
    }

    return Result.Ok(symbols)
}

fun BigDecimalError.toCalculatorError(): CalculatorError = when (this) {
    BigDecimalError.Unsupported -> CalculatorError.Unsupported
    BigDecimalError.TooLarge -> CalculatorError.ResultTooLarge
}

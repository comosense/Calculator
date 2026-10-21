package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal

internal object SymbolizerConstraints {
    const val MAX_RESULT_SIZE: Int = Constraints.MAX_RESULT_SIZE
}

enum class SymbolizerError {
    TooLarge,
    Unsupported,
}

fun BigDecimal.toSymbols(): Result<List<Symbol>, SymbolizerError> {
    val plainString: String = stripTrailingZeros().toPlainString()
    if (plainString.length > SymbolizerConstraints.MAX_RESULT_SIZE) {
        return Result.Err(SymbolizerError.TooLarge)
    }

    val symbols: List<Symbol> = buildList {
        for (c in plainString) {
            add(
                when (c) {
                    in '0'..'9' -> Symbol.Numeric.Digit(c.digitToInt())
                    '.' -> Symbol.Numeric.Point
                    '-' -> Symbol.Sign.Negative
                    else -> return Result.Err(SymbolizerError.Unsupported)
                }
            )
        }
    }

    return Result.Ok(symbols)
}

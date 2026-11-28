package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal

enum class SymbolizerError {
    TooLarge,
    Unsupported,
}

fun BigDecimal.toSymbols(
    maxLength: Long,
): Result<List<Symbol>, SymbolizerError> {
    require(maxLength >= 0L)

    val plainString: String =
        toDisplayStringOrNull(maxLength)
            ?: return Result.Err(SymbolizerError.TooLarge)

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

private fun BigDecimal.toDisplayStringOrNull(
    maxLength: Long,
): String? {
    if (signum() == 0) {
        return if (maxLength >= 1L) {
            "0"
        } else {
            null
        }
    }

    val stripped: BigDecimal = stripTrailingZeros()
    val sign: Long = if (stripped.signum() < 0) {
        1L
    } else {
        0L
    }
    val precision: Long = stripped.precision().toLong()
    val scale: Long = stripped.scale().toLong()

    val length: Long = sign + when {
        scale <= 0 -> precision - scale
        precision > scale -> precision + 1
        else -> scale + 2
    }

    return if (length <= maxLength) {
        stripped.toPlainString()
    } else {
        null
    }
}

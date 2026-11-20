package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal

internal object SymbolizerConstraints {
    const val MAX_RESULT_SIZE: Long = Constraints.MAX_RESULT_SIZE.toLong()
}

enum class SymbolizerError {
    TooLarge,
    Unsupported,
}

fun BigDecimal.toSymbols(): Result<List<Symbol>, SymbolizerError> {
    val plainString: String =
        stripTrailingZeros()
            .toPlainStringOrNull(SymbolizerConstraints.MAX_RESULT_SIZE)
            ?: return Result.Err(SymbolizerError.TooLarge)

    val symbols: List<Symbol> = buildList {
        for (c in plainString) {
            add(
                when (c) {
                    in '0'..'9' ->
                        Symbol.Numeric.Digit(c.digitToInt())

                    '.' ->
                        Symbol.Numeric.Point

                    '-' ->
                        Symbol.Sign.Negative

                    else ->
                        return Result.Err(SymbolizerError.Unsupported)
                }
            )
        }
    }

    return Result.Ok(symbols)
}

private fun BigDecimal.toPlainStringOrNull(
    maxLength: Long,
): String? {
    val sign: Long = if (signum() < 0) {
        1L
    } else {
        0
    }
    val precision: Long = precision().toLong()
    val scale: Long = scale().toLong()

    val length: Long = sign + when {
        scale <= 0 -> precision - scale
        precision > scale -> precision + 1
        else -> scale + 2
    }

    return if (length <= maxLength) {
        toPlainString()
    } else {
        null
    }
}

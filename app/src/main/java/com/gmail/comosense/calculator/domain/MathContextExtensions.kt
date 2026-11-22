package com.gmail.comosense.calculator.domain

import java.math.MathContext

internal fun MathContext.withExtraPrecision(
    extra: Int,
): MathContext {
    require(precision > 0)
    require(extra >= 0)
    require(precision <= Int.MIN_VALUE - extra)

    return MathContext(
        precision + extra,
        roundingMode,
    )
}

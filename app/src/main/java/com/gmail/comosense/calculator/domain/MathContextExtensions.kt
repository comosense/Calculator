package com.gmail.comosense.calculator.domain

import java.math.MathContext

internal fun MathContext.withExtraPrecision(
    extra: Int,
): MathContext {
    require(precision > 0)
    require(extra >= 0)

    return MathContext(
        precision + extra,
        roundingMode,
    )
}

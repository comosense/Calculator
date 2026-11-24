package com.gmail.comosense.calculator.domain

import java.math.MathContext

internal fun MathContext.withExtraPrecision(
    extra: Int,
): MathContext {
    require(precision > 0)
    require(extra >= 0)
    require(precision <= Int.MAX_VALUE - extra)

    return MathContext(
        precision + extra,
        roundingMode,
    )
}

internal fun MathContext.hasBiggerPrecisionThan(
    other: MathContext,
    extraPrecision: Int = 0,
): Boolean =
    roundingMode == other.roundingMode &&
            precision.toLong() >= (other.precision.toLong() + extraPrecision.toLong())

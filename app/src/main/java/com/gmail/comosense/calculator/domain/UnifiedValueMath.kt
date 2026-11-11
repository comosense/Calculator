package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

private const val MAX_FACTORIAL_ARGUMENT: Int =
    Constraints.MAX_FACTORIAL_ARGUMENT
private const val MAX_INTEGER_EXPONENT: Int =
    Constraints.MAX_INTEGER_EXPONENT
private const val MAX_NON_INTEGER_EXPONENT: Int =
    Constraints.MAX_NON_INTEGER_EXPONENT
private val MAX_EXPONENTIAL_ARGUMENT: BigDecimal =
    Constraints.MAX_EXPONENTIAL_ARGUMENT.toBigDecimal()
private const val ADDITIONAL_PRECISION_FOR_TRANSFORMING: Int = 8
private val NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
private val ZERO: BigDecimal = BigDecimal.ZERO
private val HALF: BigDecimal = "0.5".toBigDecimal()
private val ONE: BigDecimal = BigDecimal.ONE
private val TWO: BigDecimal = 2.toBigDecimal()
private val THREE_HALF: BigDecimal = "1.5".toBigDecimal()
private val ONE_HUNDRED_EIGHTY: BigDecimal = 180.toBigDecimal()

class FactorialLargeArgumentException : IllegalArgumentException()
class FactorialInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : IllegalArgumentException()
class ExpLargeArgumentException : IllegalArgumentException()

internal suspend fun e(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.e(
        mc = mc,
    )
)

internal suspend fun UnifiedValue.factorial(): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.factorial(
        value = factorialArgument(this),
    ),
)

internal suspend fun UnifiedValue.pow(
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    if (this.signum(mc) == 0) {
        if (exponent.signum(mc) > 0) {
            return UnifiedValue.ZERO
        }
        throw PowInvalidArgumentException()
    }

    if (exponent.signum(mc) == 0) {
        return UnifiedValue.ONE
    }

    val integerExponent: Int? = integerExponent(exponent)

    return if (integerExponent != null) {
        if (this.isPureRational()) {
            powRationalBase(
                base = this,
                exponent = integerExponent,
            )
        } else {
            powNaturalExponent(
                base = this.toBigDecimal(transformingMc(mc)),
                exponent = integerExponent,
                mc = mc,
            )
        }
    } else {
        powDecimalExponent(
            base = this.toBigDecimal(transformingMc(mc)),
            exponent = exponent,
            mc = mc,
        )
    }
}

internal suspend fun UnifiedValue.sqrt(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.sqrt(
        value = this.toBigDecimal(transformingMc(mc)),
        mc = mc,
    )
)

internal suspend fun UnifiedValue.exp(
    mc: MathContext,
): UnifiedValue {
    val value: BigDecimal = this.toBigDecimal(transformingMc(mc))

    if (value.abs() > MAX_EXPONENTIAL_ARGUMENT) {
        throw ExpLargeArgumentException()
    }

    return UnifiedValue.of(
        value = BigDecimalMath.exp(
            value = value,
            mc = mc,
        )
    )
}

internal suspend fun UnifiedValue.ln(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.ln(
        value = this.toBigDecimal(transformingMc(mc)),
        mc = mc,
    )
)

internal suspend fun UnifiedValue.log(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.log(
        value = this.toBigDecimal(transformingMc(mc)),
        mc = mc,
    )
)

internal suspend fun UnifiedValue.sin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val piMultiple: BigDecimal? = this.toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
        mc = transformingMc,
    )

    return when (piMultiple) {
        ZERO, ONE ->
            UnifiedValue.ZERO

        HALF ->
            UnifiedValue.ONE

        THREE_HALF ->
            UnifiedValue.ONE.negate()

        else ->
            UnifiedValue.of(
                value = BigDecimalMath.sin(
                    radians = this.toNormalizedRadians(
                        isRadians = isRadians,
                        mc = transformingMc,
                    ),
                    mc = mc,
                )
            )
    }
}

internal suspend fun UnifiedValue.cos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val piMultiple: BigDecimal? = this.toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
        mc = transformingMc,
    )

    return when (piMultiple) {
        HALF, THREE_HALF ->
            UnifiedValue.ZERO

        ZERO ->
            UnifiedValue.ONE

        ONE ->
            UnifiedValue.ONE.negate()

        else ->
            UnifiedValue.of(
                value = BigDecimalMath.cos(
                    radians = this.toNormalizedRadians(
                        isRadians = isRadians,
                        mc = transformingMc,
                    ),
                    mc = mc,
                )
            )
    }
}

internal suspend fun UnifiedValue.tan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val piMultiple: BigDecimal? = this.toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
        mc = transformingMc,
    )

    return when (piMultiple) {
        HALF, THREE_HALF ->
            throw TanInvalidArgumentException()

        else ->
            UnifiedValue.of(
                value = BigDecimalMath.tan(
                    radians = this.toNormalizedRadians(
                        isRadians = isRadians,
                        mc = transformingMc,
                    ),
                    mc = mc,
                )
            )

    }
}

internal suspend fun UnifiedValue.arcsin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val radians: UnifiedValue =
        when (val value: BigDecimal = this.toBigDecimal(transformingMc)) {
            ZERO ->
                UnifiedValue.ZERO

            ONE ->
                UnifiedValue.HALF_PI

            NEGATIVE_ONE ->
                UnifiedValue.HALF_PI.negate()

            else ->
                UnifiedValue.of(
                    BigDecimalMath.arcsin(
                        value = value,
                        mc = mc,
                    )
                )
        }

    return if (isRadians) {
        radians
    } else {
        radians.toDegrees(transformingMc)
    }
}

internal suspend fun UnifiedValue.arccos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val radians: UnifiedValue =
        when (val value: BigDecimal = this.toBigDecimal(transformingMc)) {
            ZERO ->
                UnifiedValue.HALF_PI

            ONE ->
                UnifiedValue.ZERO

            NEGATIVE_ONE ->
                UnifiedValue.PI

            else ->
                UnifiedValue.of(
                    BigDecimalMath.arccos(
                        value = value,
                        mc = mc,
                    )
                )
        }

    return if (isRadians) {
        radians
    } else {
        radians.toDegrees(transformingMc)
    }
}

internal suspend fun UnifiedValue.arctan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val radians: UnifiedValue =
        when (val value: BigDecimal = this.toBigDecimal(transformingMc)) {
            ZERO ->
                UnifiedValue.ZERO

            else ->
                UnifiedValue.of(
                    BigDecimalMath.arctan(
                        value = value,
                        mc = mc,
                    )
                )
        }

    return if (isRadians) {
        radians
    } else {
        radians.toDegrees(transformingMc)
    }
}

private fun factorialArgument(
    value: UnifiedValue,
): Int {
    val bigIntegerArgument: BigInteger = try {
        value.toBigIntegerExact()
    } catch (_: ArithmeticException) {
        throw FactorialInvalidArgumentException()
    }

    if (bigIntegerArgument.signum() < 0) {
        throw FactorialInvalidArgumentException()
    }

    val argument: Int = try {
        bigIntegerArgument.intValueExact()
    } catch (_: ArithmeticException) {
        throw FactorialLargeArgumentException()
    }

    if (argument > MAX_FACTORIAL_ARGUMENT) {
        throw FactorialLargeArgumentException()
    }

    return argument
}

private suspend fun powRationalBase(
    base: UnifiedValue,
    exponent: Int,
): UnifiedValue {
    return UnifiedValue.of(
        value = base.rational.pow(exponent)
    )
}

private suspend fun powNaturalExponent(
    base: BigDecimal,
    exponent: Int,
    mc: MathContext,
): UnifiedValue {
    val result: BigDecimal = BigDecimalMath.powNaturalExponent(
        base = base,
        exponent = abs(exponent),
    )

    return UnifiedValue.of(
        if (exponent >= 0) {
            result
        } else {
            BigDecimal.ONE.divide(result, mc)

        }
    )
}

private suspend fun powDecimalExponent(
    base: BigDecimal,
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    if (base.signum() < 0) {
        throw PowInvalidArgumentException()
    }

    return UnifiedValue.of(
        BigDecimalMath.powDecimalExponent(
            base = base,
            exponent = nonIntegerExponent(exponent, transformingMc(mc)),
            mc = mc,
        )
    )
}

private fun integerExponent(
    value: UnifiedValue,
): Int? {
    val bigIntegerExponent: BigInteger = try {
        value.toBigIntegerExact()
    } catch (_: ArithmeticException) {
        return null
    }

    val exponent: Int = try {
        bigIntegerExponent.intValueExact()
    } catch (_: ArithmeticException) {
        throw PowLimitExceededException()
    }

    if (exponent !in -MAX_INTEGER_EXPONENT..MAX_INTEGER_EXPONENT) {
        throw PowLimitExceededException()
    }

    return exponent
}

private suspend fun nonIntegerExponent(
    value: UnifiedValue,
    mc: MathContext,
): BigDecimal {
    val exponent: BigDecimal = value.toBigDecimal(mc)

    if (exponent.abs() > MAX_NON_INTEGER_EXPONENT.toBigDecimal()) {
        throw PowLimitExceededException()
    }

    return exponent
}

private suspend fun UnifiedValue.toNormalizedPiMultipleOrNull(
    isRadians: Boolean,
    mc: MathContext,
): BigDecimal? {

    return if (isRadians) {
        if (this.isPurePiMultiple()) {
            this.piFactor
                .toBigDecimal(transformingMc(mc))
                .remainder(TWO)
        } else {
            null
        }
    } else {
        if (this.existPiMultiple()) {
            null
        } else {
            this.toBigDecimal(transformingMc(mc))
                .divide(ONE_HUNDRED_EIGHTY, mc)
                .remainder(TWO)
        }
    }
}

private suspend fun UnifiedValue.toNormalizedRadians(
    isRadians: Boolean,
    mc: MathContext,
): BigDecimal {
    val transformingMc: MathContext = transformingMc(mc)
    val pi: BigDecimal = BigDecimalMath.pi(transformingMc)

    return if (isRadians) {
        this.toBigDecimal(transformingMc)
    } else {
        this.toBigDecimal(transformingMc)
            .multiply(pi)
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }.remainder(TWO.multiply(pi))
}

private suspend fun UnifiedValue.toDegrees(
    mc: MathContext,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)

    return UnifiedValue.of(
        value = this.toBigDecimal(transformingMc)
            .multiply(ONE_HUNDRED_EIGHTY)
            .divide(BigDecimalMath.pi(transformingMc), mc)
    )
}

private fun transformingMc(
    mc: MathContext,
): MathContext = MathContext(
    mc.precision + ADDITIONAL_PRECISION_FOR_TRANSFORMING,
    mc.roundingMode,
)



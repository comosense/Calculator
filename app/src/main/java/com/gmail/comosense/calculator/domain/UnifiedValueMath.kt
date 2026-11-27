package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.domain.Rational.Companion.toRational
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

internal class FactorialInvalidArgumentException : ArithmeticException()
internal class FactorialArgumentTooLargeException : IllegalArgumentException()
internal class PowInvalidArgumentException : ArithmeticException()
internal class PowLimitExceededException : IllegalArgumentException()
internal class ExpArgumentTooLargeException : IllegalArgumentException()
internal class TanInvalidArgumentException : ArithmeticException()

private const val MAX_DECIMAL_SCALE: Long =
    Constraints.MAX_DECIMAL_SCALE
private const val MAX_DECIMAL_BITS: Long =
    Constraints.MAX_DECIMAL_BITS
private const val MAX_FACTORIAL_ARGUMENT: Int =
    Constraints.MAX_FACTORIAL_ARGUMENT
private const val MAX_INTEGER_EXPONENT: Int =
    Constraints.MAX_INTEGER_EXPONENT
private val MAX_NON_INTEGER_EXPONENT: BigDecimal =
    Constraints.MAX_NON_INTEGER_EXPONENT.toBigDecimal()
private val MAX_EXPONENTIAL_ARGUMENT: BigDecimal =
    Constraints.MAX_EXPONENTIAL_ARGUMENT.toBigDecimal()
private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
private val NEGATIVE_ONE: UnifiedValue = UnifiedValue.ONE.negate()
private val ZERO: UnifiedValue = UnifiedValue.ZERO
private val ONE: UnifiedValue = UnifiedValue.ONE
private val BIG_DECIMAL_NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
private val BIG_DECIMAL_ZERO: BigDecimal = BigDecimal.ZERO
private val BIG_DECIMAL_ONE: BigDecimal = BigDecimal.ONE
private val BIG_DECIMAL_ONE_HUNDRED_EIGHTY: BigDecimal = 180.toBigDecimal()
private val RATIONAL_NEGATIVE_HALF: Rational = Rational.HALF.negate()
private val RATIONAL_ZERO: Rational = Rational.ZERO
private val RATIONAL_HALF: Rational = Rational.HALF
private val RATIONAL_ONE: Rational = Rational.ONE
private val RATIONAL_THREE_HALVES: Rational = Rational.ONE + Rational.HALF
private val RATIONAL_TWO: Rational = 2.toRational()
private val RATIONAL_ONE_HUNDRED_EIGHTY: Rational = 180.toRational()
private val SIN_SPECIAL_ANGLES: List<Pair<Rational, UnifiedValue>> = listOf(
    RATIONAL_ZERO to ZERO,
    RATIONAL_ONE to ZERO,
    RATIONAL_HALF to ONE,
    RATIONAL_THREE_HALVES to NEGATIVE_ONE,
)
private val COS_SPECIAL_ANGLES: List<Pair<Rational, UnifiedValue>> = listOf(
    RATIONAL_HALF to ZERO,
    RATIONAL_THREE_HALVES to ZERO,
    RATIONAL_ZERO to ONE,
    RATIONAL_ONE to NEGATIVE_ONE,
)
private val ARCSIN_SPECIAL_VALUES: List<Pair<BigDecimal, Rational>> = listOf(
    BIG_DECIMAL_ZERO to RATIONAL_ZERO,
    BIG_DECIMAL_ONE to RATIONAL_HALF,
    BIG_DECIMAL_NEGATIVE_ONE to RATIONAL_NEGATIVE_HALF,
)
private val ARCCOS_SPECIAL_VALUES: List<Pair<BigDecimal, Rational>> = listOf(
    BIG_DECIMAL_ZERO to RATIONAL_HALF,
    BIG_DECIMAL_ONE to RATIONAL_ZERO,
    BIG_DECIMAL_NEGATIVE_ONE to RATIONAL_ONE,
)
private val ARCTAN_SPECIAL_VALUES: List<Pair<BigDecimal, Rational>> = listOf(
    BIG_DECIMAL_ZERO to RATIONAL_ZERO,
)

internal suspend fun UnifiedValue.factorial(): UnifiedValue =
    UnifiedValue.fromExactValue(
        exactValue =
            BigDecimalMath.factorial(
                value = toFactorialArgument(),
            ),
    )

internal suspend fun UnifiedValue.pow(
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    if (isZero()) {
        if (exponent.signum(mc) > 0) {
            return ZERO
        }
        throw PowInvalidArgumentException()
    }

    if (exponent.isZero()) {
        return ONE
    }

    return exponent.toIntegerExponentOrNull()
        ?.let { integerExponent ->
            powIntegerExponent(
                exponent = integerExponent,
                mc = mc,
            )
        }
        ?: powUnifiedValueExponent(
            exponent = exponent,
            mc = mc,
        )
}

internal suspend fun UnifiedValue.sqrt(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue =
            BigDecimalMath.sqrt(
                value = toBigDecimal(
                    mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                ),
                mc = mc,
            ),
    )

internal suspend fun UnifiedValue.exp(
    mc: MathContext,
): UnifiedValue {
    val value: BigDecimal = toBigDecimal(
        mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
    )

    if (value.abs() > MAX_EXPONENTIAL_ARGUMENT) {
        throw ExpArgumentTooLargeException()
    }

    return UnifiedValue.fromApproximateValue(
        approximateValue =
            BigDecimalMath.exp(
                value = value,
                mc = mc,
            ),
    )
}

internal suspend fun UnifiedValue.ln(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue =
            BigDecimalMath.ln(
                value = toBigDecimal(
                    mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                ),
                mc = mc,
            ),
    )

internal suspend fun UnifiedValue.log(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue =
            BigDecimalMath.log(
                value = toBigDecimal(
                    mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                ),
                mc = mc,
            ),
    )

internal suspend fun UnifiedValue.sin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    trigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialAngles = SIN_SPECIAL_ANGLES,
        calculate = BigDecimalMath::sin,
    )

internal suspend fun UnifiedValue.cos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    trigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialAngles = COS_SPECIAL_ANGLES,
        calculate = BigDecimalMath::cos,
    )

private suspend fun UnifiedValue.trigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialAngles: List<Pair<Rational, UnifiedValue>>,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
    )?.toSpecialValueOrNull(
        specialAngles = specialAngles,
    )?.let { value -> return value }

    return UnifiedValue.fromApproximateValue(
        approximateValue =
            calculate(
                toRadians(
                    isRadians = isRadians,
                    mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                ),
                mc,
            ),
    )
}

internal suspend fun UnifiedValue.tan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val workingMc: MathContext =
        mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)

    val cos: UnifiedValue = cos(
        isRadians = isRadians,
        mc = workingMc,
    )

    if (cos.isZero()) {
        throw TanInvalidArgumentException()
    }

    val sin: UnifiedValue = sin(
        isRadians = isRadians,
        mc = workingMc,
    )

    return sin.divide(cos, mc)
}

internal suspend fun UnifiedValue.arcsin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    inverseTrigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialValues = ARCSIN_SPECIAL_VALUES,
        calculate = BigDecimalMath::arcsin,
    )

internal suspend fun UnifiedValue.arccos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    inverseTrigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialValues = ARCCOS_SPECIAL_VALUES,
        calculate = BigDecimalMath::arccos,
    )

internal suspend fun UnifiedValue.arctan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    inverseTrigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialValues = ARCTAN_SPECIAL_VALUES,
        calculate = BigDecimalMath::arctan,
    )

private suspend fun UnifiedValue.inverseTrigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialValues: List<Pair<BigDecimal, Rational>>,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    val workingMc: MathContext =
        mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
    val value: BigDecimal = toBigDecimal(workingMc)

    value.toSpecialAngleOrNull(
        isRadians = isRadians,
        specialValues = specialValues,
    )?.let { angle -> return angle }

    return UnifiedValue.fromApproximateValue(
        approximateValue =
            if (isRadians) {
                calculate(value, mc)
            } else {
                calculate(value, workingMc)
                    .multiply(BIG_DECIMAL_ONE_HUNDRED_EIGHTY)
                    .divide(BigDecimalMath.pi(workingMc), mc)
            },
    )
}

private fun UnifiedValue.toFactorialArgument(): Int {
    val bigIntegerArgument: BigInteger =
        toBigIntegerExactOrNull()
            ?: throw FactorialInvalidArgumentException()

    if (bigIntegerArgument.signum() < 0) {
        throw FactorialInvalidArgumentException()
    }

    val integerArgument: Int = try {
        bigIntegerArgument.intValueExact()
    } catch (_: ArithmeticException) {
        throw FactorialArgumentTooLargeException()
    }

    if (integerArgument > MAX_FACTORIAL_ARGUMENT) {
        throw FactorialArgumentTooLargeException()
    }

    return integerArgument
}

private fun UnifiedValue.toIntegerExponentOrNull(): Int? {
    val bigIntegerExponent: BigInteger =
        toBigIntegerExactOrNull()
            ?: return null

    val integerExponent: Int = try {
        bigIntegerExponent.intValueExact()
    } catch (_: ArithmeticException) {
        throw PowLimitExceededException()
    }

    if (integerExponent !in
        -MAX_INTEGER_EXPONENT..MAX_INTEGER_EXPONENT
    ) {
        throw PowLimitExceededException()
    }

    return integerExponent
}

private suspend fun UnifiedValue.powIntegerExponent(
    exponent: Int,
    mc: MathContext,
): UnifiedValue {
    require(exponent != Int.MIN_VALUE)

    toBigIntegerExactOrNull()?.let { base ->
        val result: UnifiedValue = base.powIntegerExponentOrNull(
            exponent = exponent,
            mc = mc,
        ) ?: throw CalculationLimitExceededException()

        return result
    }

    toRationalOrNull()?.let { base ->
        val result: UnifiedValue = base.powIntegerExponentOrNull(
            exponent = exponent,
        ) ?: return@let

        return result
    }

    val bigDecimalBase: BigDecimal =
        toBigDecimal(mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING))

    return bigDecimalBase.powIntegerExponentOrNull(
        exponent = exponent,
        mc = mc,
    ) ?: throw CalculationLimitExceededException()
}

private suspend fun UnifiedValue.powUnifiedValueExponent(
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    val workingMc: MathContext =
        mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
    val bigDecimalBase: BigDecimal = toBigDecimal(workingMc)

    if (bigDecimalBase.signum() < 0) {
        throw PowInvalidArgumentException()
    }

    val bigDecimalExponent: BigDecimal =
        exponent.toBigDecimal(workingMc)

    if (bigDecimalExponent.abs() > MAX_NON_INTEGER_EXPONENT) {
        throw PowLimitExceededException()
    }

    return UnifiedValue.fromApproximateValue(
        approximateValue =
            BigDecimalMath.powDecimalExponent(
                base = bigDecimalBase,
                exponent = bigDecimalExponent,
                mc = mc,
            ),
    )
}

private suspend fun BigInteger.powIntegerExponentOrNull(
    exponent: Int,
    mc: MathContext,
): UnifiedValue? {
    val estimatedBits: Long =
        bitLength().toLong() * abs(exponent.toLong())

    if (estimatedBits > MAX_DECIMAL_BITS) {
        return null
    }

    val result: BigInteger = BigDecimalMath.powNaturalExponent(
        base = this,
        exponent = abs(exponent),
    )

    return if (exponent >= 0) {
        UnifiedValue.fromExactValue(
            exactValue = result,
        )
    } else {
        UnifiedValue.fromApproximateValue(
            approximateValue = BIG_DECIMAL_ONE
                .divide(result.toBigDecimal(), mc),
        )
    }
}

private suspend fun Rational.powIntegerExponentOrNull(
    exponent: Int,
): UnifiedValue? =
    powOrNull(exponent)?.let { result ->
        UnifiedValue.fromApproximateValue(
            approximateValue = result,
        )
    }

private suspend fun BigDecimal.powIntegerExponentOrNull(
    exponent: Int,
    mc: MathContext,
): UnifiedValue? {
    val result: BigDecimal = BigDecimalMath.powNaturalExponent(
        base = this,
        exponent = abs(exponent),
        mc =
            if (exponent >= 0) {
                mc
            } else {
                mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
            },
    )

    if (abs(result.scale().toLong()) > MAX_DECIMAL_SCALE) {
        return null
    }

    return UnifiedValue.fromApproximateValue(
        approximateValue =
            if (exponent >= 0) {
                result
            } else {
                BIG_DECIMAL_ONE.divide(result, mc)
            },
    )
}

private fun UnifiedValue.toNormalizedPiMultipleOrNull(
    isRadians: Boolean,
): Rational? =
    if (isRadians) {
        toPiMultipleOrNull()
    } else {
        toRationalOrNull()
            ?.div(RATIONAL_ONE_HUNDRED_EIGHTY)
    }?.modulo(RATIONAL_TWO)

private suspend fun UnifiedValue.toRadians(
    isRadians: Boolean,
    mc: MathContext,
): BigDecimal =
    if (isRadians) {
        toBigDecimal(mc)
    } else {
        val workingMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)

        toBigDecimal(workingMc)
            .multiply(BigDecimalMath.pi(workingMc))
            .divide(BIG_DECIMAL_ONE_HUNDRED_EIGHTY, mc)
    }

private fun Rational.toSpecialValueOrNull(
    specialAngles: List<Pair<Rational, UnifiedValue>>,
): UnifiedValue? =
    specialAngles
        .firstOrNull { (key, _) -> this == key }
        ?.second

private fun BigDecimal.toSpecialAngleOrNull(
    isRadians: Boolean,
    specialValues: List<Pair<BigDecimal, Rational>>,
): UnifiedValue? =
    specialValues
        .firstOrNull { (key, _) -> compareTo(key) == 0 }
        ?.second
        ?.let { value ->
            if (isRadians) {
                UnifiedValue.fromPiCoefficient(
                    piCoefficient = value,
                )
            } else {
                UnifiedValue.fromApproximateValue(
                    approximateValue = value * RATIONAL_ONE_HUNDRED_EIGHTY,
                )
            }
        }

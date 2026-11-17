package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

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
private val RATIONAL_THREE_HALF: Rational = Rational.ONE + Rational.HALF
private val RATIONAL_TWO: Rational = Rational.ONE + Rational.ONE
private val RATIONAL_ONE_HUNDRED_EIGHTY: Rational = Rational.from(180.toBigDecimal())

class FactorialInvalidArgumentException : ArithmeticException()
class FactorialLargeArgumentException : IllegalArgumentException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : IllegalArgumentException()
class ExpLargeArgumentException : IllegalArgumentException()
class TanInvalidArgumentException : ArithmeticException()

internal suspend fun UnifiedValue.factorial(): UnifiedValue =
    UnifiedValue.fromExactValue(
        exactValue = BigDecimalMath.factorial(
            value = factorialArgument(this),
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

    return integerExponentOrNull(exponent)
        ?.let { integerExponent ->
            pow(
                exponent = integerExponent,
                mc = mc,
            )
        }
        ?: powDecimalExponent(
            base = this,
            exponent = exponent,
            mc = mc,
        )
}

internal suspend fun UnifiedValue.sqrt(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue = BigDecimalMath.sqrt(
            value = toBigDecimal(workingMc(mc)),
            mc = mc,
        ),
    )

internal suspend fun UnifiedValue.exp(
    mc: MathContext,
): UnifiedValue {
    val value: BigDecimal = toBigDecimal(workingMc(mc))
    if (value.abs() > MAX_EXPONENTIAL_ARGUMENT) {
        throw ExpLargeArgumentException()
    }

    return UnifiedValue.fromApproximateValue(
        approximateValue = BigDecimalMath.exp(
            value = value,
            mc = mc,
        ),
    )
}

internal suspend fun UnifiedValue.ln(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue = BigDecimalMath.ln(
            value = toBigDecimal(workingMc(mc)),
            mc = mc,
        ),
    )

internal suspend fun UnifiedValue.log(
    mc: MathContext,
): UnifiedValue =
    UnifiedValue.fromApproximateValue(
        approximateValue = BigDecimalMath.log(
            value = toBigDecimal(workingMc(mc)),
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
        specialOrNull = ::sinSpecialOrNull,
        calculate = BigDecimalMath::sin,
    )

internal suspend fun UnifiedValue.cos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    trigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialOrNull = ::cosSpecialOrNull,
        calculate = BigDecimalMath::cos,
    )

private suspend fun UnifiedValue.trigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialOrNull: (Rational) -> UnifiedValue?,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
    )?.let(specialOrNull)
        ?.let { result -> return result }

    return UnifiedValue.fromApproximateValue(
        approximateValue = calculate(
            toRadians(
                isRadians = isRadians,
                mc = workingMc(mc),
            ),
            mc,
        ),
    )
}

internal suspend fun UnifiedValue.tan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val workingMc: MathContext = workingMc(mc)

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
        specialOrNull = ::arcsinSpecialOrNull,
        calculate = BigDecimalMath::arcsin,
    )

internal suspend fun UnifiedValue.arccos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    inverseTrigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialOrNull = ::arccosSpecialOrNull,
        calculate = BigDecimalMath::arccos,
    )

internal suspend fun UnifiedValue.arctan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue =
    inverseTrigonometricFunction(
        isRadians = isRadians,
        mc = mc,
        specialOrNull = ::arctanSpecialOrNull,
        calculate = BigDecimalMath::arctan,
    )

private suspend fun UnifiedValue.inverseTrigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialOrNull: (BigDecimal, Boolean) -> UnifiedValue?,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    val workingMc: MathContext = workingMc(mc)
    val value: BigDecimal = toBigDecimal(workingMc)

    specialOrNull(value, isRadians)
        ?.let { special -> return special }

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

private fun factorialArgument(
    value: UnifiedValue,
): Int {
    val bigIntegerArgument: BigInteger =
        value.toBigIntegerExactOrNull()
            ?: throw FactorialInvalidArgumentException()

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

private fun integerExponentOrNull(
    value: UnifiedValue,
): Int? {
    val bigIntegerExponent: BigInteger =
        value.toBigIntegerExactOrNull()
            ?: return null

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

private suspend fun powDecimalExponent(
    base: UnifiedValue,
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    val workingMc: MathContext = workingMc(mc)

    val bigDecimalBase: BigDecimal = base.toBigDecimal(workingMc)
    if (bigDecimalBase.signum() < 0) {
        throw PowInvalidArgumentException()
    }

    val bigDecimalExponent: BigDecimal = exponent.toBigDecimal(workingMc(mc))
    if (bigDecimalExponent.abs() > MAX_NON_INTEGER_EXPONENT) {
        throw PowLimitExceededException()
    }

    return UnifiedValue.fromApproximateValue(
        approximateValue = BigDecimalMath.powDecimalExponent(
            base = bigDecimalBase,
            exponent = bigDecimalExponent,
            mc = mc,
        ),
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
        val workingMc: MathContext = workingMc(mc)
        toBigDecimal(workingMc)
            .multiply(BigDecimalMath.pi(workingMc))
            .divide(BIG_DECIMAL_ONE_HUNDRED_EIGHTY, mc)
    }

private fun sinSpecialOrNull(
    piMultiple: Rational,
): UnifiedValue? =
    when (piMultiple) {
        RATIONAL_ZERO, RATIONAL_ONE ->
            ZERO

        RATIONAL_HALF ->
            ONE

        RATIONAL_THREE_HALF ->
            NEGATIVE_ONE

        else ->
            null
    }

private fun cosSpecialOrNull(
    piMultiple: Rational,
): UnifiedValue? =
    when (piMultiple) {
        RATIONAL_HALF, RATIONAL_THREE_HALF ->
            ZERO

        RATIONAL_ZERO ->
            ONE

        RATIONAL_ONE ->
            NEGATIVE_ONE

        else ->
            null
    }

private fun arcsinSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? =
    when {
        value.compareTo(BIG_DECIMAL_ZERO) == 0 ->
            RATIONAL_ZERO

        value.compareTo(BIG_DECIMAL_ONE) == 0 ->
            RATIONAL_HALF

        value.compareTo(BIG_DECIMAL_NEGATIVE_ONE) == 0 ->
            RATIONAL_NEGATIVE_HALF

        else ->
            null
    }?.toAngleUnifiedValue(
        isRadians = isRadians,
    )

private fun arccosSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? =
    when {
        value.compareTo(BIG_DECIMAL_ZERO) == 0 ->
            RATIONAL_HALF

        value.compareTo(BIG_DECIMAL_ONE) == 0 ->
            RATIONAL_ZERO

        value.compareTo(BIG_DECIMAL_NEGATIVE_ONE) == 0 ->
            RATIONAL_ONE

        else ->
            null
    }?.toAngleUnifiedValue(
        isRadians = isRadians,
    )

private fun arctanSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? =
    when {
        value.compareTo(BIG_DECIMAL_ZERO) == 0 ->
            RATIONAL_ZERO

        else ->
            null
    }?.toAngleUnifiedValue(
        isRadians = isRadians,
    )

private fun Rational.toAngleUnifiedValue(
    isRadians: Boolean,
): UnifiedValue =
    if (isRadians) {
        UnifiedValue.fromPiCoefficient(
            piCoefficient = this,
        )
    } else {
        UnifiedValue.fromApproximateValue(
            approximateValue = this * Rational.from(BIG_DECIMAL_ONE_HUNDRED_EIGHTY),
        )
    }

private fun workingMc(
    mc: MathContext,
): MathContext =
    MathContext(
        mc.precision + EXTRA_PRECISION_FOR_WORKING,
        mc.roundingMode,
    )

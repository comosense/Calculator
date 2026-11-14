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
private val MAX_NON_INTEGER_EXPONENT: BigDecimal =
    Constraints.MAX_NON_INTEGER_EXPONENT.toBigDecimal()
private val MAX_EXPONENTIAL_ARGUMENT: BigDecimal =
    Constraints.MAX_EXPONENTIAL_ARGUMENT.toBigDecimal()
private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
private val NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
private val ZERO: BigDecimal = BigDecimal.ZERO
private val HALF: BigDecimal = "0.5".toBigDecimal()
private val ONE: BigDecimal = BigDecimal.ONE
private val THREE_HALF: BigDecimal = "1.5".toBigDecimal()
private val TWO: BigDecimal = 2.toBigDecimal()
private val ONE_HUNDRED_EIGHTY: BigDecimal = 180.toBigDecimal()

class FactorialInvalidArgumentException : ArithmeticException()
class FactorialLargeArgumentException : IllegalArgumentException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : IllegalArgumentException()
class ExpLargeArgumentException : IllegalArgumentException()
class TanInvalidArgumentException : ArithmeticException()

internal suspend fun UnifiedValue.factorial(): UnifiedValue = UnifiedValue.from(
    bigInteger = BigDecimalMath.factorial(
        value = factorialArgument(this),
    ),
)

internal suspend fun UnifiedValue.pow(
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    if (signum(mc) == 0) {
        if (exponent.signum(mc) > 0) {
            return UnifiedValue.ZERO
        }
        throw PowInvalidArgumentException()
    }

    if (exponent.signum(mc) == 0) {
        return UnifiedValue.ONE
    }

    return integerExponentOrNull(exponent)
        ?.let { integerExponent ->
            powNaturalExponent(
                base = this,
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
): UnifiedValue = UnifiedValue.from(
    bigDecimal = BigDecimalMath.sqrt(
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

    return UnifiedValue.from(
        bigDecimal = BigDecimalMath.exp(
            value = value,
            mc = mc,
        ),
    )
}

internal suspend fun UnifiedValue.ln(
    mc: MathContext,
): UnifiedValue = UnifiedValue.from(
    bigDecimal = BigDecimalMath.ln(
        value = toBigDecimal(workingMc(mc)),
        mc = mc,
    ),
)

internal suspend fun UnifiedValue.log(
    mc: MathContext,
): UnifiedValue = UnifiedValue.from(
    bigDecimal = BigDecimalMath.log(
        value = toBigDecimal(workingMc(mc)),
        mc = mc,
    ),
)

internal suspend fun UnifiedValue.sin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = trigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialOrNull = ::sinSpecialOrNull,
    calculate = BigDecimalMath::sin,
)

internal suspend fun UnifiedValue.cos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = trigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialOrNull = ::cosSpecialOrNull,
    calculate = BigDecimalMath::cos,
)

private suspend fun UnifiedValue.trigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialOrNull: (BigDecimal) -> UnifiedValue?,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    val workingMc: MathContext = workingMc(mc)

    toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
        mc = workingMc,
    )?.let(specialOrNull)
        ?.let { result -> return result }

    return UnifiedValue.from(
        bigDecimal = calculate(
            toRadians(
                isRadians = isRadians,
                mc = workingMc,
            ),
            mc,
        ),
    )
}

internal suspend fun UnifiedValue.tan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue {
    val workingMc = workingMc(mc)

    val cos: UnifiedValue = cos(
        isRadians = isRadians,
        mc = workingMc,
    )

    if (cos.signum(mc) == 0) {
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
): UnifiedValue = inverseTrigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialOrNull = ::arcsinSpecialOrNull,
    calculate = BigDecimalMath::arcsin,
)

internal suspend fun UnifiedValue.arccos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = inverseTrigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialOrNull = ::arccosSpecialOrNull,
    calculate = BigDecimalMath::arccos,
)

internal suspend fun UnifiedValue.arctan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = inverseTrigonometricFunction(
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

    return if (isRadians) {
        UnifiedValue.from(
            bigDecimal = calculate(value, mc),
        )
    } else {
        UnifiedValue.from(
            bigDecimal = calculate(value, workingMc)
                .multiply(ONE_HUNDRED_EIGHTY)
                .divide(BigDecimalMath.pi(workingMc), mc),
        )
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

private fun integerExponentOrNull(
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

private suspend fun powNaturalExponent(
    base: UnifiedValue,
    exponent: Int,
    mc: MathContext,
): UnifiedValue {
    return if (base.isExactRational()) {
        powRationalBase(
            base = base,
            exponent = exponent,
        )
    } else {
        powDecimalBase(
            base = base.toBigDecimal(workingMc(mc)),
            exponent = exponent,
            mc = mc,
        )
    }
}

private suspend fun powDecimalExponent(
    base: UnifiedValue,
    exponent: UnifiedValue,
    mc: MathContext,
): UnifiedValue {
    val workingMc: MathContext = workingMc(mc)

    val bigDecimalBase = base.toBigDecimal(workingMc)
    if (bigDecimalBase.signum() < 0) {
        throw PowInvalidArgumentException()
    }

    val bigDecimalExponent: BigDecimal = exponent.toBigDecimal(workingMc(mc))
    if (bigDecimalExponent.abs() > MAX_NON_INTEGER_EXPONENT) {
        throw PowLimitExceededException()
    }

    return UnifiedValue.from(
        bigDecimal = BigDecimalMath.powDecimalExponent(
            base = bigDecimalBase,
            exponent = bigDecimalExponent,
            mc = mc,
        ),
    )
}

private suspend fun powRationalBase(
    base: UnifiedValue,
    exponent: Int,
): UnifiedValue {
    return UnifiedValue.from(
        rational = base.rational.pow(exponent),
    )
}

private suspend fun powDecimalBase(
    base: BigDecimal,
    exponent: Int,
    mc: MathContext,
): UnifiedValue {
    val result: BigDecimal = BigDecimalMath.powNaturalExponent(
        base = base,
        exponent = abs(exponent),
    )

    return UnifiedValue.from(
        bigDecimal = if (exponent >= 0) {
            result
        } else {
            BigDecimal.ONE.divide(result, mc)
        },
    )
}

private suspend fun UnifiedValue.toNormalizedPiMultipleOrNull(
    isRadians: Boolean,
    mc: MathContext,
): BigDecimal? {
    return if (isRadians) {
        toPiMultipleOrNull()
            ?.toBigDecimal(workingMc(mc))
    } else {
        if (hasPiFactor()) {
            null
        } else {
            toBigDecimal(workingMc(mc))
                .divide(ONE_HUNDRED_EIGHTY, mc)
        }
    }?.remainder(TWO, mc)
        ?.let { normalizedPiMultiple ->
            if (normalizedPiMultiple.signum() < 0) {
                normalizedPiMultiple.add(TWO)
            } else {
                normalizedPiMultiple
            }
        }
}

private suspend fun UnifiedValue.toRadians(
    isRadians: Boolean,
    mc: MathContext,
): BigDecimal {
    return if (isRadians) {
        toBigDecimal(mc)
    } else {
        val workingMc: MathContext = workingMc(mc)

        toBigDecimal(workingMc)
            .multiply(BigDecimalMath.pi(workingMc))
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }
}

private fun sinSpecialOrNull(
    piMultiple: BigDecimal,
): UnifiedValue? = when {
    piMultiple.compareTo(ZERO) == 0 ||
            piMultiple.compareTo(ONE) == 0 ->
        UnifiedValue.ZERO

    piMultiple.compareTo(HALF) == 0 ->
        UnifiedValue.ONE

    piMultiple.compareTo(THREE_HALF) == 0 ->
        UnifiedValue.NEGATIVE_ONE

    else ->
        null
}

private fun cosSpecialOrNull(
    piMultiple: BigDecimal,
): UnifiedValue? = when {
    piMultiple.compareTo(HALF) == 0 ||
            piMultiple.compareTo(THREE_HALF) == 0 ->
        UnifiedValue.ZERO

    piMultiple.compareTo(ZERO) == 0 ->
        UnifiedValue.ONE

    piMultiple.compareTo(ONE) == 0 ->
        UnifiedValue.NEGATIVE_ONE

    else ->
        null
}

private fun arcsinSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when {
    value.compareTo(ZERO) == 0 ->
        Rational.ZERO

    value.compareTo(ONE) == 0 ->
        Rational.HALF

    value.compareTo(NEGATIVE_ONE) == 0 ->
        Rational.NEGATIVE_HALF

    else ->
        null
}?.toAngleUnifiedValue(
    isRadians = isRadians,
)

private fun arccosSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when {
    value.compareTo(ZERO) == 0 ->
        Rational.HALF

    value.compareTo(ONE) == 0 ->
        Rational.ZERO

    value.compareTo(NEGATIVE_ONE) == 0 ->
        Rational.ONE

    else ->
        null
}?.toAngleUnifiedValue(
    isRadians = isRadians,
)

private fun arctanSpecialOrNull(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when {
    value.compareTo(ZERO) == 0 ->
        Rational.ZERO

    else ->
        null
}?.toAngleUnifiedValue(
    isRadians = isRadians,
)

private fun Rational.toAngleUnifiedValue(
    isRadians: Boolean,
): UnifiedValue {
    return if (isRadians) {
        UnifiedValue.fromPiFactor(
            piFactor = this,
        )
    } else {
        UnifiedValue.from(
            rational = this * Rational.from(ONE_HUNDRED_EIGHTY)
        )
    }
}

private fun workingMc(
    mc: MathContext,
): MathContext = MathContext(
    mc.precision + EXTRA_PRECISION_FOR_WORKING,
    mc.roundingMode,
)

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
private val NINETY: BigDecimal = 90.toBigDecimal()
private val ONE_HUNDRED_EIGHTY: BigDecimal = 180.toBigDecimal()

class FactorialLargeArgumentException : IllegalArgumentException()
class FactorialInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : IllegalArgumentException()
class ExpLargeArgumentException : IllegalArgumentException()

internal suspend fun UnifiedValue.factorial(): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.factorial(
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

    val integerExponent: Int? = integerExponent(exponent)

    return if (integerExponent != null) {
        if (isPureRational()) {
            powRationalBase(
                base = this,
                exponent = integerExponent,
            )
        } else {
            powNaturalExponent(
                base = toBigDecimal(transformingMc(mc)),
                exponent = integerExponent,
                mc = mc,
            )
        }
    } else {
        powDecimalExponent(
            base = toBigDecimal(transformingMc(mc)),
            exponent = exponent,
            mc = mc,
        )
    }
}

internal suspend fun UnifiedValue.sqrt(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.sqrt(
        value = toBigDecimal(transformingMc(mc)),
        mc = mc,
    ),
)

internal suspend fun UnifiedValue.exp(
    mc: MathContext,
): UnifiedValue {
    val value: BigDecimal = toBigDecimal(transformingMc(mc))

    if (value.abs() > MAX_EXPONENTIAL_ARGUMENT) {
        throw ExpLargeArgumentException()
    }

    return UnifiedValue.of(
        value = BigDecimalMath.exp(
            value = value,
            mc = mc,
        ),
    )
}

internal suspend fun UnifiedValue.ln(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.ln(
        value = toBigDecimal(transformingMc(mc)),
        mc = mc,
    ),
)

internal suspend fun UnifiedValue.log(
    mc: MathContext,
): UnifiedValue = UnifiedValue.of(
    value = BigDecimalMath.log(
        value = toBigDecimal(transformingMc(mc)),
        mc = mc,
    ),
)

internal suspend fun UnifiedValue.sin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = trigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::sinSpecialResult,
    calculate = BigDecimalMath::sin,
)

internal suspend fun UnifiedValue.cos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = trigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::cosSpecialResult,
    calculate = BigDecimalMath::cos,
)

private suspend fun UnifiedValue.trigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialResult: (BigDecimal) -> UnifiedValue?,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)

    toNormalizedPiMultipleOrNull(
        isRadians = isRadians,
        mc = transformingMc,
    )?.let(specialResult)
        ?.let { result -> return result }

    return UnifiedValue.of(
        value = calculate(
            toNormalizedRadians(
                isRadians = isRadians,
                mc = transformingMc,
            ),
            mc,
        ),
    )
}

internal suspend fun UnifiedValue.tan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = trigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::tanSpecialResult,
    calculate = BigDecimalMath::tan,
)

internal suspend fun UnifiedValue.arcsin(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = inverseTrigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::arcsinSpecialResult,
    calculate = BigDecimalMath::arcsin,
)

internal suspend fun UnifiedValue.arccos(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = inverseTrigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::arccosSpecialResult,
    calculate = BigDecimalMath::arccos,
)

internal suspend fun UnifiedValue.arctan(
    isRadians: Boolean,
    mc: MathContext,
): UnifiedValue = inverseTrigonometricFunction(
    isRadians = isRadians,
    mc = mc,
    specialResult = ::arctanSpecialResult,
    calculate = BigDecimalMath::arctan,
)

private suspend fun UnifiedValue.inverseTrigonometricFunction(
    isRadians: Boolean,
    mc: MathContext,
    specialResult: (BigDecimal, Boolean) -> UnifiedValue?,
    calculate: suspend (BigDecimal, MathContext) -> BigDecimal,
): UnifiedValue {
    val transformingMc: MathContext = transformingMc(mc)
    val value: BigDecimal = toBigDecimal(transformingMc)

    specialResult(value, isRadians)
        ?.let { result -> return result }

    return if (isRadians) {
        UnifiedValue.of(calculate(value, mc))
    } else {
        UnifiedValue.of(calculate(value, transformingMc).toDegrees(mc))
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
        value = base.rational.pow(exponent),
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
        value = if (exponent >= 0) {
            result
        } else {
            BigDecimal.ONE.divide(result, mc)
        },
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
        value = BigDecimalMath.powDecimalExponent(
            base = base,
            exponent = nonIntegerExponent(exponent, transformingMc(mc)),
            mc = mc,
        ),
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
        if (isPurePiMultiple()) {
            piFactor
                .toBigDecimal(transformingMc(mc))
                .remainder(TWO)
        } else {
            null
        }
    } else {
        if (isIrrational()) {
            null
        } else {
            toBigDecimal(transformingMc(mc))
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
        toBigDecimal(transformingMc)
    } else {
        toBigDecimal(transformingMc)
            .multiply(pi)
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }.remainder(TWO.multiply(pi))
}

private suspend fun BigDecimal.toDegrees(
    mc: MathContext,
): BigDecimal = multiply(ONE_HUNDRED_EIGHTY)
    .divide(BigDecimalMath.pi(transformingMc(mc)), mc)

private fun sinSpecialResult(
    piMultiple: BigDecimal,
): UnifiedValue? = when (piMultiple) {
    ZERO, ONE ->
        UnifiedValue.ZERO

    HALF ->
        UnifiedValue.ONE

    THREE_HALF ->
        UnifiedValue.ONE.negate()

    else ->
        null
}

private fun cosSpecialResult(
    piMultiple: BigDecimal,
): UnifiedValue? = when (piMultiple) {
    HALF, THREE_HALF ->
        UnifiedValue.ZERO

    ZERO ->
        UnifiedValue.ONE

    ONE ->
        UnifiedValue.ONE.negate()

    else ->
        null
}

private fun tanSpecialResult(
    piMultiple: BigDecimal,
): UnifiedValue? = when (piMultiple) {
    HALF, THREE_HALF ->
        throw TanInvalidArgumentException()

    else ->
        null
}

private fun arcsinSpecialResult(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when (value) {
    ZERO ->
        if (isRadians) {
            UnifiedValue.ZERO
        } else {
            UnifiedValue.of(ZERO)
        }

    ONE ->
        if (isRadians) {
            UnifiedValue.HALF_PI
        } else {
            UnifiedValue.of(NINETY)
        }

    NEGATIVE_ONE ->
        if (isRadians) {
            UnifiedValue.HALF_PI.negate()
        } else {
            UnifiedValue.of(ZERO).negate()
        }

    else ->
        null
}

private fun arccosSpecialResult(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when (value) {
    ZERO ->
        if (isRadians) {
            UnifiedValue.HALF_PI
        } else {
            UnifiedValue.of(NINETY)
        }

    ONE ->
        if (isRadians) {
            UnifiedValue.ZERO
        } else {
            UnifiedValue.of(ZERO)
        }

    NEGATIVE_ONE ->
        if (isRadians) {
            UnifiedValue.PI
        } else {
            UnifiedValue.of(ONE_HUNDRED_EIGHTY)
        }

    else ->
        null
}

private fun arctanSpecialResult(
    value: BigDecimal,
    isRadians: Boolean,
): UnifiedValue? = when (value) {
    ZERO ->
        if (isRadians) {
            UnifiedValue.ZERO
        } else {
            UnifiedValue.of(ZERO)
        }

    else ->
        null
}

private fun transformingMc(
    mc: MathContext,
): MathContext = MathContext(
    mc.precision + ADDITIONAL_PRECISION_FOR_TRANSFORMING,
    mc.roundingMode,
)

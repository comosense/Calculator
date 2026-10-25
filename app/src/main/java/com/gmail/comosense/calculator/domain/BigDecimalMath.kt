package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

internal object BigDecimalMathConstraints {
    const val MIN_PRECISION: Int = Constraints.MIN_PRECISION
    const val MAX_FACTORIAL_ARGUMENT: Int = Constraints.MAX_FACTORIAL_ARGUMENT
    const val MAX_INTEGER_EXPONENT: Int = Constraints.MAX_INTEGER_EXPONENT
    const val MAX_NON_INTEGER_EXPONENT: Int = Constraints.MAX_NON_INTEGER_EXPONENT
}

class FactorialLargeArgumentException : ArithmeticException()
class FactorialInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : ArithmeticException()

class BigDecimalMath(private val mathContext: MathContext) {
    init {
        require(mathContext.precision >= BigDecimalMathConstraints.MIN_PRECISION)
    }

    companion object {
        private const val EXTRA_PRECISION_FOR_CONSTANT: Int = 2
        private const val EXTRA_PRECISION_FOR_BIG_DECIMAL_MATH_CORE: Int = 8
        private const val TOLERANCE_PRECISION_MARGIN: Int = 2
        private val ZERO: BigDecimal = BigDecimal.ZERO
        private val ONE: BigDecimal = BigDecimal.ONE
        private val MAX_FACTORIAL_ARGUMENT: BigInteger =
            BigDecimalMathConstraints.MAX_FACTORIAL_ARGUMENT.toBigInteger()
        private val MAX_INTEGER_EXPONENT: BigInteger =
            BigDecimalMathConstraints.MAX_INTEGER_EXPONENT.toBigInteger()
        private val MAX_NON_INTEGER_EXPONENT: BigDecimal =
            BigDecimal(BigDecimalMathConstraints.MAX_NON_INTEGER_EXPONENT)
    }

    private val mathContextForConstant: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION_FOR_CONSTANT,
        mathContext.roundingMode,
    )
    private val coreMc: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION_FOR_BIG_DECIMAL_MATH_CORE,
        mathContext.roundingMode,
    )
    private val core = BigDecimalMathCore(
        coreMc = coreMc,
        tolerancePrecision = mathContext.precision - TOLERANCE_PRECISION_MARGIN,
    )

    suspend fun e(): BigDecimal {
        return core.e()
            .round(mathContextForConstant)
    }

    suspend fun pi(): BigDecimal {
        return core.pi()
            .round(mathContextForConstant)
    }

    suspend fun factorial(value: BigDecimal): BigDecimal {
        val integerValue: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue.signum() < 0) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue > MAX_FACTORIAL_ARGUMENT) {
            throw FactorialLargeArgumentException()
        }

        return core.factorialUnrounded(integerValue)
    }

    suspend fun sqrt(value: BigDecimal): BigDecimal {
        return core.sqrt(value)
            .round(mathContext)
    }

    suspend fun pow(
        base: BigDecimal,
        exponent: BigDecimal,
    ): BigDecimal {
        if (base.signum() == 0) {
            return if (exponent.signum() > 0) {
                ZERO
            } else {
                throw PowInvalidArgumentException()
            }
        }

        if (exponent.signum() == 0) {
            return ONE
        }

        val integerExponent: BigInteger? = try {
            exponent.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }

        return if (integerExponent != null) {
            if (integerExponent.abs() > MAX_INTEGER_EXPONENT) {
                throw PowLimitExceededException()
            }
            val result =
                core.powUnrounded(base, integerExponent.abs().intValueExact())
            if (integerExponent.signum() > 0) {
                result
            } else {
                ONE.divide(result, mathContext)
            }
        } else {
            if (exponent.abs() > MAX_NON_INTEGER_EXPONENT) {
                throw PowLimitExceededException()
            }
            if (base.signum() < 0) {
                throw PowInvalidArgumentException()
            }
            core.pow(base, exponent)
                .round(mathContext)
        }
    }

    suspend fun ln(value: BigDecimal): BigDecimal {
        return core.ln(value)
            .round(mathContext)
    }

    suspend fun log(value: BigDecimal): BigDecimal {
        return core.log(value)
            .round(mathContext)
    }

    suspend fun sin(angle: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.sin(angle, isRadians)
            .round(mathContext)
    }

    suspend fun cos(angle: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.cos(angle, isRadians)
            .round(mathContext)
    }

    suspend fun tan(angle: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.tan(angle, isRadians)
            .round(mathContext)
    }

    suspend fun arcsin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.arcsin(value, isRadians)
            .round(mathContext)
    }

    suspend fun arccos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.arccos(value, isRadians)
            .round(mathContext)
    }

    suspend fun arctan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return core.arctan(value, isRadians)
            .round(mathContext)
    }
}

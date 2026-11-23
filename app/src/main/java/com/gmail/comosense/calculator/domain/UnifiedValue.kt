package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.domain.Rational.Companion.toRational
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

internal class DivisionByZeroException : ArithmeticException()
internal class CalculationLimitExceededException : ArithmeticException()

class UnifiedValue private constructor(
    val exactValue: BigDecimal = BigDecimal.ZERO,
    val approximateValue: Rational = Rational.ZERO,
    val piCoefficient: Rational = Rational.ZERO,
) {
    companion object {
        private const val MAX_INTERMEDIATE_PRECISION =
            Constraints.MAX_INTERMEDIATE_PRECISION
        private const val MAX_DECIMAL_SCALE: Long =
            Constraints.MAX_DECIMAL_SCALE
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
        private val BIG_DECIMAL_ZERO: BigDecimal = BigDecimal.ZERO

        val ZERO: UnifiedValue = UnifiedValue()
        val ONE: UnifiedValue = UnifiedValue(
            exactValue = BigDecimal.ONE,
        )

        fun fromExactValue(
            exactValue: BigDecimal,
        ): UnifiedValue =
            UnifiedValue(
                exactValue = exactValue,
            )

        fun fromExactValue(
            exactValue: BigInteger,
        ): UnifiedValue =
            UnifiedValue(
                exactValue = exactValue.toBigDecimal(),
            )

        fun fromApproximateValue(
            approximateValue: BigDecimal,
        ): UnifiedValue =
            UnifiedValue(
                approximateValue = approximateValue.toRational(),
            )

        fun fromApproximateValue(
            approximateValue: Rational,
        ): UnifiedValue =
            UnifiedValue(
                approximateValue = approximateValue,
            )

        fun fromPiCoefficient(
            piCoefficient: Rational,
        ): UnifiedValue =
            UnifiedValue(
                piCoefficient = piCoefficient,
            )

        fun pi(): UnifiedValue =
            fromPiCoefficient(
                piCoefficient = Rational.ONE,
            )

        suspend fun e(
            mc: MathContext,
        ): UnifiedValue =
            fromApproximateValue(
                approximateValue =
                    BigDecimalMath.e(
                        mc = mc,
                    ),
            )
    }

    fun add(
        other: UnifiedValue,
    ): UnifiedValue {
        if (other.isZero()) {
            return this
        }

        if (isZero()) {
            return other
        }

        return UnifiedValue(
            exactValue = exactValue.add(other.exactValue),
            approximateValue = approximateValue + other.approximateValue,
            piCoefficient = piCoefficient + other.piCoefficient,
        )
    }

    fun subtract(
        other: UnifiedValue,
    ): UnifiedValue =
        add(other = other.negate())

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        if (other.isZero() || isZero()) {
            return ZERO
        }

        val newExactValue: BigDecimal =
            if (!hasExactValue() || !other.hasExactValue()) {
                BIG_DECIMAL_ZERO
            } else {
                if ((exactValue.precision() + other.exactValue.precision())
                    > MAX_INTERMEDIATE_PRECISION
                ) {
                    throw CalculationLimitExceededException()
                }

                val product: BigDecimal =
                    exactValue
                        .multiply(other.exactValue)
                        .stripTrailingZeros()

                if (abs(product.scale().toLong()) > MAX_DECIMAL_SCALE) {
                    throw CalculationLimitExceededException()
                }

                product
            }

        val piSquaredPartValue: Rational =
            if (hasPiCoefficient() && other.hasPiCoefficient()) {
                val workingMc: MathContext =
                    mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
                val pi: BigDecimal = BigDecimalMath.pi(workingMc)
                val piSquared: Rational = pi.multiply(pi, mc).toRational()

                piCoefficient * other.piCoefficient * piSquared
            } else {
                Rational.ZERO
            }
        val exactValueRational: Rational = exactValue.toRational()
        val otherExactValueRational: Rational = other.exactValue.toRational()

        val newApproximateValue: Rational =
            approximateValue * other.approximateValue +
                    exactValueRational * other.approximateValue +
                    approximateValue * otherExactValueRational +
                    piSquaredPartValue

        val newPiCoefficient: Rational =
            (exactValueRational + approximateValue) * other.piCoefficient +
                    (otherExactValueRational + other.approximateValue) * piCoefficient

        return UnifiedValue(
            exactValue = newExactValue,
            approximateValue = newApproximateValue,
            piCoefficient = newPiCoefficient,
        )
    }

    suspend fun divide(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        if (other.isZero()) {
            throw DivisionByZeroException()
        }

        if (isZero()) {
            return ZERO
        }

        other.toRationalOrNull()?.let { denominator ->
            return UnifiedValue(
                approximateValue =
                    (exactValue.toRational() + approximateValue) / denominator,
                piCoefficient =
                    piCoefficient / denominator,
            )
        }

        val workingMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)

        return UnifiedValue(
            approximateValue =
                toBigDecimal(workingMc)
                    .divide(other.toBigDecimal(workingMc), mc)
                    .toRational(),
        )
    }

    fun negate(): UnifiedValue =
        UnifiedValue(
            exactValue = exactValue.negate(),
            approximateValue = approximateValue.negate(),
            piCoefficient = piCoefficient.negate(),
        )

    suspend fun signum(
        mc: MathContext,
    ): Int =
        toRationalOrNull()?.signum()
            ?: toBigDecimal(mc).signum()

    fun isZero(): Boolean {
        if (hasPiCoefficient()) {
            return false
        }

        if (!hasExactValue() && !hasApproximateValue()) {
            return true
        }

        if (!hasExactValue() || !hasApproximateValue()) {
            return false
        }

        return rationalValue().isZero()
    }

    suspend fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        if (!hasApproximateValue() && !hasPiCoefficient()) {
            return exactValue
        }

        if (!hasPiCoefficient()) {
            if (!hasExactValue()) {
                return approximateValue.toBigDecimal(mc)
            }

            val rationalValue: Rational = rationalValue()
            if (rationalValue.isZero()) {
                return BigDecimal.ZERO
            }

            if (rationalValue.abs() < approximateValue.abs()) {
                return rationalValue.toBigDecimal(mc)
            }

            return exactValue.add(approximateValue.toBigDecimal(mc))
        }

        val workingMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
        val piTerm: BigDecimal =
            piCoefficient.toBigDecimal(workingMc)
                .multiply(BigDecimalMath.pi(workingMc), workingMc)

        if (hasExactValue() &&
            hasApproximateValue()
        ) {
            val rationalValue: Rational = rationalValue()
            if (rationalValue.isZero() ||
                rationalValue.abs() < approximateValue.abs()
            ) {
                return rationalValue.toBigDecimal(workingMc)
                    .add(piTerm)
                    .round(mc)
            }
        }

        val approximatePlusPiPartValue: BigDecimal =
            approximateValue.toBigDecimal(workingMc)
                .add(piTerm)
        val sumWithExact: BigDecimal =
            exactValue.add(approximatePlusPiPartValue)

        return if (
            hasExactValue() &&
            exactValue.signum() != approximatePlusPiPartValue.signum() &&
            sumWithExact.abs() < approximatePlusPiPartValue.abs()
        ) {
            sumWithExact.round(mc)
        } else {
            exactValue.add(approximatePlusPiPartValue.round(mc))
        }
    }

    fun toBigIntegerExactOrNull(): BigInteger? =
        toRationalOrNull()?.toBigIntegerExactOrNull()

    fun toRationalOrNull(): Rational? =
        if (!hasPiCoefficient()) {
            rationalValue()
        } else {
            null
        }

    fun toPiMultipleOrNull(): Rational? {
        if (!hasPiCoefficient()) {
            return null
        }

        if (!hasExactValue() && !hasApproximateValue()) {
            return piCoefficient
        }

        if (!hasExactValue() || !hasApproximateValue()) {
            return null
        }

        return if (rationalValue().isZero()) {
            piCoefficient
        } else {
            null
        }
    }

    private fun hasExactValue(): Boolean =
        exactValue.signum() != 0

    private fun hasApproximateValue(): Boolean =
        !approximateValue.isZero()

    private fun hasPiCoefficient(): Boolean =
        !piCoefficient.isZero()

    private fun rationalValue(): Rational =
        exactValue.toRational() + approximateValue
}

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
        private const val MAX_INTERMEDIATE_PRECISION = Constraints.MAX_INTERMEDIATE_PRECISION
        private const val MAX_DECIMAL_SCALE: Long = Constraints.MAX_DECIMAL_SCALE
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
                approximateValue = BigDecimalMath.e(
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

        val leftExactValueRational: Rational =
            exactValue.toRational()
        val rightExactValueRational: Rational =
            other.exactValue.toRational()

        var newApproximateValue: Rational =
            approximateValue * other.approximateValue +
                    leftExactValueRational * other.approximateValue +
                    rightExactValueRational * approximateValue

        if (hasPiCoefficient() && other.hasPiCoefficient()) {
            val workingMc: MathContext =
                mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)
            val pi: BigDecimal = BigDecimalMath.pi(workingMc)
            val piSquared: BigDecimal = pi.multiply(pi, workingMc)

            newApproximateValue +=
                (piCoefficient * other.piCoefficient)
                    .toBigDecimal(workingMc)
                    .multiply(piSquared, mc)
                    .toRational()
        }

        val newPiCoefficient: Rational =
            (leftExactValueRational + approximateValue) * other.piCoefficient +
                    (rightExactValueRational + other.approximateValue) * piCoefficient

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

        val denominator: Rational? = other.toRationalOrNull()

        if (denominator != null) {
            return UnifiedValue(
                approximateValue = (exactValue.toRational() + approximateValue) / denominator,
                piCoefficient = piCoefficient / denominator,
            )
        }

        val workingMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)

        return UnifiedValue(
            approximateValue = toBigDecimal(workingMc)
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

    fun isZero(): Boolean =
        !hasExactValue() &&
                !hasApproximateValue() &&
                !hasPiCoefficient()

    suspend fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        if (!hasApproximateValue() && !hasPiCoefficient()) {
            return exactValue
        }

        val roundedValue: BigDecimal = if (!hasPiCoefficient()) {
            approximateValue
                .toBigDecimal(mc)
        } else {
            val workingMc: MathContext =
                mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING)

            approximateValue
                .toBigDecimal(workingMc)
                .add(
                    piCoefficient.toBigDecimal(workingMc)
                        .multiply(
                            BigDecimalMath.pi(workingMc),
                            workingMc
                        )
                ).round(mc)
        }

        return exactValue.add(roundedValue)
    }

    fun toBigIntegerExactOrNull(): BigInteger? =
        toRationalOrNull()?.toBigIntegerExactOrNull()

    fun toRationalOrNull(): Rational? =
        if (!hasPiCoefficient()) {
            approximateValue + exactValue.toRational()
        } else {
            null
        }

    fun toPiMultipleOrNull(): Rational? =
        if (!hasExactValue() &&
            !hasApproximateValue() &&
            hasPiCoefficient()
        ) {
            piCoefficient
        } else {
            null
        }

    private fun hasExactValue(): Boolean =
        exactValue.signum() != 0

    private fun hasApproximateValue(): Boolean =
        !approximateValue.isZero()

    private fun hasPiCoefficient(): Boolean =
        !piCoefficient.isZero()
}

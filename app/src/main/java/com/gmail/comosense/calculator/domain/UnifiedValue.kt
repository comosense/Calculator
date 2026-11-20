package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.domain.Rational.Companion.toRational
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

class DivisionByZeroException : ArithmeticException()

class UnifiedValue private constructor(
    val exactValue: BigDecimal = BigDecimal.ZERO,
    val approximateValue: Rational = Rational.ZERO,
    val piCoefficient: Rational = Rational.ZERO,
) {
    companion object {
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 2

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
        maxPrecisionMc: MathContext,
    ): UnifiedValue {
        if (other.isZero()) {
            return this
        }
        if (isZero()) {
            return other
        }

        return UnifiedValue(
            exactValue = exactValue.add(other.exactValue, maxPrecisionMc),
            approximateValue = approximateValue + other.approximateValue,
            piCoefficient = piCoefficient + other.piCoefficient,
        )
    }

    fun subtract(
        other: UnifiedValue,
        maxPrecisionMc: MathContext,
    ): UnifiedValue =
        add(
            other = other.negate(),
            maxPrecisionMc = maxPrecisionMc,
        )

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
        maxPrecisionMc: MathContext,
    ): UnifiedValue {
        require(maxPrecisionMc.precision >= mc.precision)

        if (other.isZero() || isZero()) {
            return ZERO
        }

        val newExactValue: BigDecimal =
            exactValue.multiply(other.exactValue, maxPrecisionMc)

        val leftExactValueRational: Rational =
            exactValue.toRational()
        val rightExactValueRational: Rational =
            other.exactValue.toRational()

        var newApproximateValue: Rational =
            approximateValue * other.approximateValue +
                    leftExactValueRational * other.approximateValue +
                    rightExactValueRational * approximateValue

        if (hasPiCoefficient() && other.hasPiCoefficient()) {
            val workingMc: MathContext = workingMc(mc)

            newApproximateValue +=
                (piCoefficient * other.piCoefficient)
                    .toBigDecimal(workingMc)
                    .multiply(
                        BigDecimalMath.powNaturalExponent(
                            base = BigDecimalMath.pi(workingMc),
                            exponent = 2,
                        ),
                        mc,
                    ).toRational()
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

        val workingMc: MathContext = workingMc(mc)

        return UnifiedValue(
            approximateValue = toBigDecimal(workingMc)
                .divide(other.toBigDecimal(workingMc), mc)
                .toRational(),
        )
    }

    suspend fun pow(
        exponent: Int,
        mc: MathContext,
    ): UnifiedValue {
        require(exponent != Int.MIN_VALUE)

        if (isZero()) {
            require(exponent >= 0)
            return if (exponent == 0) {
                ONE
            } else {
                ZERO
            }
        }

        if (!hasExactValue() && !hasPiCoefficient()) {
            return fromApproximateValue(
                approximateValue = approximateValue.pow(exponent),
            )
        }

        val isExactValue: Boolean = !hasApproximateValue() && !hasPiCoefficient()
        val base: BigDecimal = if (isExactValue) {
            exactValue
        } else {
            toBigDecimal(workingMc(mc))
        }
        val result: BigDecimal = BigDecimalMath.powNaturalExponent(
            base = base,
            exponent = abs(exponent),
        )

        return when {
            exponent < 0 ->
                fromApproximateValue(
                    approximateValue = BigDecimal.ONE
                        .divide(result, mc),
                )

            isExactValue ->
                fromExactValue(
                    exactValue = result,
                )

            else ->
                fromApproximateValue(
                    approximateValue = result,
                )
        }
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
            val workingMc: MathContext = workingMc(mc)
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

    private fun workingMc(
        mc: MathContext,
    ): MathContext {
        require(mc.precision > 0)

        return MathContext(
            mc.precision + EXTRA_PRECISION_FOR_WORKING,
            mc.roundingMode,
        )
    }
}

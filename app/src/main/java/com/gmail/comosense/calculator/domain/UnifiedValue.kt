package com.gmail.comosense.calculator.domain

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

        val NEGATIVE_ONE: UnifiedValue = UnifiedValue(
            exactValue = BigDecimal.ONE.negate(),
        )
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
                approximateValue = Rational.from(approximateValue),
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

    fun hasPiCoefficient(): Boolean =
        !piCoefficient.isZero()

    fun isZero(): Boolean =
        !hasExactValue() &&
                !hasApproximateValue() &&
                !hasPiCoefficient()

    fun toPiMultipleOrNull(): Rational? =
        if (!hasExactValue() &&
            !hasApproximateValue() &&
            hasPiCoefficient()
        ) {
            piCoefficient
        } else {
            null
        }

    suspend fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        var value: BigDecimal =
            exactValue.add(approximateValue.toBigDecimal(mc))

        if (hasPiCoefficient()) {
            val workingMc: MathContext = workingMc(mc)

            value = value.add(
                piCoefficient.toBigDecimal(workingMc)
                    .multiply(BigDecimalMath.pi(workingMc))
            ).round(mc)
        }

        return value
    }

    fun toBigIntegerExactOrNull(): BigInteger? =
        toRationalOrNull()?.toBigIntegerExactOrNull()

    suspend fun signum(
        mc: MathContext,
    ): Int =
        toRationalOrNull()?.signum()
            ?: toBigDecimal(mc).signum()

    fun negate(): UnifiedValue =
        UnifiedValue(
            exactValue = exactValue.negate(),
            approximateValue = approximateValue.negate(),
            piCoefficient = piCoefficient.negate(),
        )

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
        add(other.negate())

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        if (other.isZero() || isZero()) {
            return ZERO
        }

        val newExactValue: BigDecimal =
            exactValue.multiply(other.exactValue)

        val leftExactValueRational: Rational =
            Rational.from(exactValue)
        val rightExactValueRational: Rational =
            Rational.from(other.exactValue)

        var newApproximateValue: Rational =
            approximateValue * other.approximateValue +
                    leftExactValueRational * other.approximateValue +
                    rightExactValueRational * approximateValue

        if (hasPiCoefficient() && !other.hasPiCoefficient()) {
            val workingMc: MathContext = workingMc(mc)

            newApproximateValue += Rational.from(
                (piCoefficient * other.piCoefficient).toBigDecimal(workingMc)
                    .multiply(
                        BigDecimalMath.powNaturalExponent(
                            base = BigDecimalMath.pi(workingMc),
                            exponent = 2,
                        )
                    )
                    .round(mc),
            )
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

        return if (denominator != null) {
            UnifiedValue(
                approximateValue = (Rational.from(exactValue) + approximateValue) / denominator,
                piCoefficient = piCoefficient / denominator,
            )
        } else {
            val workingMc: MathContext = workingMc(mc)
            val approximateDenominator: BigDecimal = other.toBigDecimal(workingMc)

            UnifiedValue(
                approximateValue = Rational.from(
                    toBigDecimal(workingMc)
                        .divide(approximateDenominator, mc)
                ),
            )
        }
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
                    approximateValue = BigDecimal.ONE.divide(result, mc),
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

    private fun hasExactValue(): Boolean =
        exactValue.signum() != 0

    private fun hasApproximateValue(): Boolean =
        !approximateValue.isZero()

    private fun toRationalOrNull(): Rational? =
        if (!hasPiCoefficient()) {
            approximateValue + Rational.from(exactValue)
        } else {
            null
        }

    private fun workingMc(
        mc: MathContext,
    ): MathContext =
        MathContext(
            mc.precision + EXTRA_PRECISION_FOR_WORKING,
            mc.roundingMode,
        )
}

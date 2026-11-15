package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

class DivisionByZeroException : ArithmeticException()

class UnifiedValue private constructor(
    val exactValue: BigDecimal = BigDecimal.ZERO,
    val approximateValue: Rational = Rational.ZERO,
    val piFactor: Rational = Rational.ZERO,
) {
    companion object {
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
        val NEGATIVE_ONE: UnifiedValue = UnifiedValue(
            exactValue = BigDecimal.ONE,
        ).negate()
        val ZERO: UnifiedValue = UnifiedValue(
            exactValue = BigDecimal.ZERO,
        )
        val ONE: UnifiedValue = UnifiedValue(
            exactValue = BigDecimal.ONE,
        )

        fun fromExactValue(
            exactValue: BigDecimal,
        ): UnifiedValue = UnifiedValue(
            exactValue = exactValue,
        )

        fun fromExactValue(
            exactValue: BigInteger,
        ): UnifiedValue = UnifiedValue(
            exactValue = exactValue.toBigDecimal(),
        )

        fun fromApproximateValue(
            approximateValue: BigDecimal,
        ): UnifiedValue = UnifiedValue(
            approximateValue = Rational.from(approximateValue),
        )

        fun fromApproximateValue(
            approximateValue: Rational,
        ): UnifiedValue = UnifiedValue(
            approximateValue = approximateValue,
        )

        fun fromPiFactor(
            piFactor: Rational,
        ): UnifiedValue = UnifiedValue(
            piFactor = piFactor,
        )

        fun pi(): UnifiedValue = fromPiFactor(
            piFactor = Rational.ONE,
        )

        suspend fun e(
            mc: MathContext,
        ): UnifiedValue = fromApproximateValue(
            approximateValue = BigDecimalMath.e(
                mc = mc,
            ),
        )
    }

    fun hasPiFactor(): Boolean =
        !piFactor.isZero()

    fun toPiMultipleOrNull(): Rational? {
        return if (exactValue.signum() == 0 &&
            approximateValue.isZero() &&
            !piFactor.isZero()
        ) {
            piFactor
        } else {
            null
        }
    }

    suspend fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        var value: BigDecimal =
            exactValue.add(approximateValue.toBigDecimal(mc))

        if (hasPiFactor()) {
            val workingMc: MathContext = workingMc(mc)

            value = value.add(
                piFactor.toBigDecimal(workingMc)
                    .multiply(BigDecimalMath.pi(workingMc))
            ).round(mc)
        }

        return value
    }

    fun toBigIntegerExactOrNull(): BigInteger? =
        toRationalOrNull()?.toBigIntegerExactOrNull()

    suspend fun signum(
        mc: MathContext,
    ): Int = toRationalOrNull()?.signum()
        ?: toBigDecimal(mc).signum()

    fun negate(): UnifiedValue = UnifiedValue(
        exactValue = exactValue.negate(),
        approximateValue = approximateValue.negate(),
        piFactor = piFactor.negate(),
    )

    fun add(
        other: UnifiedValue,
    ): UnifiedValue {
        if (other.isZero()) {
            return this
        }

        return UnifiedValue(
            exactValue = exactValue.add(other.exactValue),
            approximateValue = approximateValue + other.approximateValue,
            piFactor = piFactor + other.piFactor,
        )
    }

    fun subtract(
        other: UnifiedValue,
    ): UnifiedValue {
        if (other.isZero()) {
            return this
        }

        return UnifiedValue(
            exactValue = exactValue.subtract(other.exactValue),
            approximateValue = approximateValue - other.approximateValue,
            piFactor = piFactor - other.piFactor,
        )
    }

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        if (isZero() || other.isZero()) {
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

        if (!piFactor.isZero() && !other.piFactor.isZero()) {
            val workingMc: MathContext = workingMc(mc)

            newApproximateValue += Rational.from(
                (piFactor * other.piFactor).toBigDecimal(workingMc)
                    .multiply(
                        BigDecimalMath.powNaturalExponent(
                            base = BigDecimalMath.pi(workingMc),
                            exponent = 2,
                        )
                    )
                    .round(mc)
            )
        }

        val newPiFactor: Rational =
            (leftExactValueRational + approximateValue) * other.piFactor +
                    (rightExactValueRational + other.approximateValue) * piFactor

        return UnifiedValue(
            exactValue = newExactValue,
            approximateValue = newApproximateValue,
            piFactor = newPiFactor,
        )
    }

    suspend fun divide(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        if (other.isZero()) {
            throw DivisionByZeroException()
        }

        val denominator: Rational? = other.toRationalOrNull()

        return if (denominator != null) {
            UnifiedValue(
                approximateValue = (Rational.from(exactValue) + approximateValue) / denominator,
                piFactor = piFactor / denominator,
            )
        } else {
            val workingMc: MathContext = workingMc(mc)
            val approximateDenominator: BigDecimal = other.toBigDecimal(workingMc)

            UnifiedValue(
                approximateValue = Rational.from(
                    toBigDecimal(workingMc)
                        .divide(approximateDenominator, mc)
                )
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
            return when {
                exponent > 0 -> ZERO
                else -> ONE
            }
        }

        return if (exactValue.signum() == 0 && piFactor.isZero()) {
            fromApproximateValue(
                approximateValue = approximateValue.pow(exponent),
            )
        } else {
            val isExactValue: Boolean = approximateValue.isZero() && piFactor.isZero()
            val base: BigDecimal = if (isExactValue) {
                exactValue
            } else {
                toBigDecimal(workingMc(mc))
            }
            val result: BigDecimal = BigDecimalMath.powNaturalExponent(
                base = base,
                exponent = abs(exponent),
            )

            if (exponent >= 0) {
                if (isExactValue) {
                    fromExactValue(
                        exactValue = result,
                    )
                } else {
                    fromApproximateValue(
                        approximateValue = result,
                    )
                }
            } else {
                fromApproximateValue(
                    approximateValue = BigDecimal.ONE.divide(result, mc),
                )
            }
        }
    }

    private fun isZero(): Boolean =
        exactValue.signum() == 0 &&
                approximateValue.isZero() &&
                piFactor.isZero()

    private fun toRationalOrNull(): Rational? {
        return if (!hasPiFactor()) {
            approximateValue + Rational.from(exactValue)
        } else {
            null
        }
    }

    private fun workingMc(
        mc: MathContext,
    ): MathContext = MathContext(
        mc.precision + EXTRA_PRECISION_FOR_WORKING,
        mc.roundingMode,
    )
}

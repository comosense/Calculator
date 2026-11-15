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
                    .round(mc)
            )
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
    ): UnifiedValue = UnifiedValue(
        exactValue = exactValue.add(other.exactValue),
        approximateValue = approximateValue + other.approximateValue,
        piFactor = piFactor + other.piFactor,
    )

    fun subtract(
        other: UnifiedValue,
    ): UnifiedValue = UnifiedValue(
        exactValue = exactValue.subtract(other.exactValue),
        approximateValue = approximateValue - other.approximateValue,
        piFactor = piFactor - other.piFactor,
    )

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        val leftExactValueRational: Rational =
            Rational.from(exactValue)
        val rightExactValueRational: Rational =
            Rational.from(other.exactValue)

        var newExactValue: BigDecimal =
            exactValue.multiply(other.exactValue)

        if (!piFactor.isZero() && !other.piFactor.isZero()) {
            val workingMc: MathContext = workingMc(mc)

            newExactValue = newExactValue.add(
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

        val newApproximateValue: Rational =
            approximateValue * other.approximateValue +
                    leftExactValueRational * other.approximateValue +
                    rightExactValueRational * approximateValue

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
        val denominator: Rational? = other.toRationalOrNull()

        return if (denominator != null) {
            if (denominator.isZero()) {
                throw DivisionByZeroException()
            }

            UnifiedValue(
                approximateValue = (Rational.from(exactValue) + approximateValue) / denominator,
                piFactor = piFactor / denominator,
            )
        } else {
            val workingMc: MathContext = workingMc(mc)

            val approximateDenominator: BigDecimal = other.toBigDecimal(workingMc)
            if (approximateDenominator.signum() == 0) {
                throw DivisionByZeroException()
            }

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
        return if (hasOnlyApproximateValue()) {
            fromApproximateValue(
                approximateValue = approximateValue.pow(exponent),
            )
        } else {
            val result: BigDecimal = BigDecimalMath.powNaturalExponent(
                base = toBigDecimal(workingMc(mc)),
                exponent = abs(exponent),
            )

            if (exponent >= 0) {
                fromExactValue(
                    exactValue = result,
                )
            } else {
                fromApproximateValue(
                    approximateValue = BigDecimal.ONE.divide(result, mc),
                )
            }
        }
    }

    private fun toRationalOrNull(): Rational? {
        return if (hasOnlyApproximateValue()) {
            approximateValue
        } else if (!hasPiFactor()) {
            approximateValue + Rational.from(exactValue)
        } else {
            null
        }
    }

    private fun hasOnlyApproximateValue(): Boolean =
        exactValue.signum() == 0 && piFactor.isZero()

    private fun workingMc(
        mc: MathContext,
    ): MathContext = MathContext(
        mc.precision + EXTRA_PRECISION_FOR_WORKING,
        mc.roundingMode,
    )
}

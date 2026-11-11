package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class DivisionByZeroException : ArithmeticException()

class UnifiedValue private constructor(
    val decimal: BigDecimal = BigDecimal.ZERO,
    val rational: Rational = Rational.ZERO,
    val piFactor: Rational = Rational.ZERO,
) {
    companion object {
        val ZERO: UnifiedValue = UnifiedValue(
            rational = Rational.ZERO,
        )

        val ONE: UnifiedValue = UnifiedValue(
            rational = Rational.ONE,
        )

        val HALF_PI: UnifiedValue = UnifiedValue(
            piFactor = Rational.HALF
        )
        val PI: UnifiedValue = UnifiedValue(
            piFactor = Rational.ONE,
        )

        fun of(value: BigDecimal): UnifiedValue = UnifiedValue(
            decimal = value,
        )

        fun of(value: BigInteger): UnifiedValue = UnifiedValue(
            decimal = value.toBigDecimal(),
        )

        fun of(value: Rational): UnifiedValue = UnifiedValue(
            rational = value,
        )
    }

    private fun toExactRationalOrNull(): Rational? {
        return if (isPureRational()) {
            rational
        } else if (piFactor.isZero()) {
            rational + Rational.of(decimal)
        } else {
            null
        }
    }

    private suspend fun toApproximateRational(
        mc: MathContext,
    ): Rational {
        return rational +
                Rational.of(
                    decimal +
                            piFactor.toBigDecimal(mc)
                                .multiply(BigDecimalMath.pi(mc))
                )
    }

    fun isPureRational(): Boolean =
        decimal.signum() == 0 &&
                piFactor.isZero()

    fun isPurePiMultiple(): Boolean =
        decimal.signum() == 0 &&
                rational.isZero() &&
                !piFactor.isZero()

    fun existPiMultiple(): Boolean =
        !piFactor.isZero()

    suspend fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        var result: BigDecimal = decimal.add(rational.toBigDecimal(mc))

        if (existPiMultiple()) {
            result = result.add(
                piFactor.toBigDecimal(mc)
                    .multiply(BigDecimalMath.pi(mc))
            )
        }

        return result
    }

    fun toBigIntegerExact(): BigInteger =
        toExactRationalOrNull()?.toBigIntegerExact()
            ?: throw ArithmeticException()

    suspend fun signum(
        mc: MathContext,
    ): Int =
        toExactRationalOrNull()?.signum()
            ?: toBigDecimal(mc).signum()

    fun negate(): UnifiedValue = UnifiedValue(
        decimal = decimal.negate(),
        rational = rational.negate(),
        piFactor = piFactor.negate(),
    )

    fun add(
        other: UnifiedValue,
    ): UnifiedValue = UnifiedValue(
        decimal = decimal.add(other.decimal),
        rational = rational + other.rational,
        piFactor = piFactor + other.piFactor,
    )

    fun subtract(
        other: UnifiedValue,
    ): UnifiedValue = UnifiedValue(
        decimal = decimal.subtract(other.decimal),
        rational = rational - other.rational,
        piFactor = piFactor - other.piFactor,
    )

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        val leftDecimalRational: Rational = Rational.of(decimal)
        val rightDecimalRational: Rational = Rational.of(other.decimal)

        var newDecimal: BigDecimal =
            decimal.multiply(other.decimal)

        if (!piFactor.isZero() && !other.piFactor.isZero()) {
            newDecimal = newDecimal.add(
                (piFactor * other.piFactor).toBigDecimal(mc)
                    .multiply(
                        BigDecimalMath.powNaturalExponent(
                            base = BigDecimalMath.pi(mc),
                            exponent = 2,
                        )
                    )
            )
        }

        val newRational: Rational =
            rational * other.rational +
                    leftDecimalRational * other.rational +
                    rightDecimalRational * rational

        val newPiFactor: Rational =
            (leftDecimalRational + rational) * other.piFactor +
                    (rightDecimalRational + other.rational) * piFactor

        return UnifiedValue(
            decimal = newDecimal,
            rational = newRational,
            piFactor = newPiFactor,
        )
    }

    suspend fun divide(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        val denominator: Rational? = other.toExactRationalOrNull()

        return if (denominator != null && !denominator.isZero()) {
            if (denominator.isZero()) {
                throw DivisionByZeroException()
            }

            UnifiedValue(
                rational = (Rational.of(decimal) + rational) / denominator,
                piFactor = piFactor / denominator,
            )
        } else {
            val approximateDenominator: Rational = other.toApproximateRational(mc)
            if (approximateDenominator.isZero()) {
                throw DivisionByZeroException()
            }

            UnifiedValue(
                rational = toApproximateRational(mc) / approximateDenominator
            )
        }
    }
}

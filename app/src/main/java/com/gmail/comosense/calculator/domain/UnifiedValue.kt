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
        val NEGATIVE_ONE: UnifiedValue = UnifiedValue(
            rational = Rational.ONE,
        ).negate()
        val ZERO: UnifiedValue = UnifiedValue(
            rational = Rational.ZERO,
        )
        val ONE: UnifiedValue = UnifiedValue(
            rational = Rational.ONE,
        )

        fun from(
            bigDecimal: BigDecimal,
        ): UnifiedValue = UnifiedValue(
            decimal = bigDecimal,
        )

        fun from(
            bigInteger: BigInteger,
        ): UnifiedValue = UnifiedValue(
            decimal = bigInteger.toBigDecimal(),
        )

        fun from(
            rational: Rational,
        ): UnifiedValue = UnifiedValue(
            rational = rational,
        )

        fun fromPiFactor(
            piFactor: Rational,
        ) = UnifiedValue(
            piFactor = piFactor,
        )

        fun pi(): UnifiedValue = fromPiFactor(
            piFactor = Rational.ONE,
        )

        suspend fun e(
            mc: MathContext,
        ): UnifiedValue = from(
            bigDecimal = BigDecimalMath.e(
                mc = mc,
            ),
        )
    }

    private fun toExactRationalOrNull(): Rational? {
        return if (isPureRational()) {
            rational
        } else if (piFactor.isZero()) {
            rational + Rational.from(decimal)
        } else {
            null
        }
    }

    private suspend fun toApproximateRational(
        mc: MathContext,
    ): Rational {
        return rational +
                Rational.from(
                    decimal +
                            piFactor.toBigDecimal(mc)
                                .multiply(BigDecimalMath.pi(mc))
                )
    }

    fun isPureRational(): Boolean =
        decimal.signum() == 0 &&
                piFactor.isZero()

    fun isIrrational(): Boolean =
        !piFactor.isZero()

    fun toPiMultipleOrNull(): Rational? {
        return if (decimal.signum() == 0 &&
            rational.isZero() &&
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
        var result: BigDecimal = decimal.add(rational.toBigDecimal(mc))

        if (isIrrational()) {
            result = result.add(
                piFactor.toBigDecimal(mc)
                    .multiply(BigDecimalMath.pi(mc), mc)
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
        val leftDecimalRational: Rational = Rational.from(decimal)
        val rightDecimalRational: Rational = Rational.from(other.decimal)

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

        return if (denominator != null) {
            if (denominator.isZero()) {
                throw DivisionByZeroException()
            }

            UnifiedValue(
                rational = (Rational.from(decimal) + rational) / denominator,
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

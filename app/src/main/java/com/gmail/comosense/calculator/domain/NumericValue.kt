package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class DivisionByZeroException : ArithmeticException()

class NumericValue private constructor(
    val decimal: BigDecimal = BigDecimal.ZERO,
    val rational: Rational = Rational.ZERO,
) {
    companion object {
        val ZERO: NumericValue = NumericValue(
            rational = Rational.ZERO,
        )

        val ONE: NumericValue = NumericValue(
            rational = Rational.ONE,
        )

        fun of(value: BigDecimal): NumericValue = NumericValue(
            decimal = value,
        )

        fun of(value: BigInteger): NumericValue = NumericValue(
            decimal = value.toBigDecimal(),
        )

        fun of(value: Rational): NumericValue = NumericValue(
            rational = value,
        )
    }

    private fun toRational(): Rational =
        Rational.of(decimal) + rational

    fun toBigDecimal(
        mc: MathContext,
    ): BigDecimal {
        return decimal.add(rational.toBigDecimal(mc))
    }

    fun toBigIntegerExact(): BigInteger =
        toRational().toBigIntegerExact()

    fun signum(): Int =
        toRational().signum()

    fun negate(): NumericValue = NumericValue(
        decimal = decimal.negate(),
        rational = rational.negate(),
    )

    operator fun plus(
        other: NumericValue,
    ): NumericValue = NumericValue(
        decimal = decimal.add(other.decimal),
        rational = rational + other.rational,
    )

    operator fun minus(
        other: NumericValue,
    ): NumericValue = NumericValue(
        decimal = decimal.subtract(other.decimal),
        rational = rational - other.rational,
    )

    operator fun times(
        other: NumericValue,
    ): NumericValue {
        val leftDecimal: Rational = Rational.of(decimal)
        val rightDecimal: Rational = Rational.of(other.decimal)

        return NumericValue(
            decimal = decimal.multiply(other.decimal),
            rational = leftDecimal * other.rational +
                    rational * rightDecimal +
                    rational * other.rational,
        )
    }

    operator fun div(
        other: NumericValue,
    ): NumericValue {
        val denominator: Rational = other.toRational()

        if (denominator.isZero()) {
            throw DivisionByZeroException()
        }

        return NumericValue(
            rational = toRational() / denominator,
        )
    }
}

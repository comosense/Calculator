package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class DivisionByZeroException : ArithmeticException()

class NumericValue private constructor(
    val decimal: BigDecimal,
    val rational: Rational,
) {
    companion object {
        val ZERO: NumericValue = of(
            rational = Rational.ZERO,
        )

        val ONE: NumericValue = of(
            rational = Rational.ONE,
        )

        fun from(value: BigDecimal): NumericValue = of(
            decimal = value,
        )

        fun from(value: BigInteger): NumericValue = of(
            decimal = value.toBigDecimal(),
        )

        fun of(
            decimal: BigDecimal = BigDecimal.ZERO,
            rational: Rational = Rational.ZERO,
        ): NumericValue = NumericValue(
            decimal = decimal,
            rational = rational,
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

    fun negate(): NumericValue = of(
        decimal = decimal.negate(),
        rational = rational.negate(),
    )

    operator fun plus(
        other: NumericValue,
    ): NumericValue = of(
        decimal = decimal.add(other.decimal),
        rational = rational + other.rational,
    )

    operator fun minus(
        other: NumericValue,
    ): NumericValue = of(
        decimal = decimal.subtract(other.decimal),
        rational = rational - other.rational,
    )

    operator fun times(
        other: NumericValue,
    ): NumericValue {
        val leftDecimal: Rational = Rational.of(decimal)
        val rightDecimal: Rational = Rational.of(other.decimal)

        return of(
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

        return of(
            rational = toRational() / denominator,
        )
    }
}

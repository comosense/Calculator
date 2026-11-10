package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs

class Rational private constructor(
    val numerator: BigInteger,
    val denominator: BigInteger,
) {
    init {
        require(denominator.signum() > 0)
    }

    companion object {
        val ZERO: Rational = Rational(
            numerator = BigInteger.ZERO,
            denominator = BigInteger.ONE,
        )
        val ONE: Rational = Rational(
            numerator = BigInteger.ONE,
            denominator = BigInteger.ONE,
        )

        fun of(
            numerator: BigInteger,
            denominator: BigInteger = BigInteger.ONE,
        ): Rational {
            require(denominator.signum() != 0)

            if (numerator.signum() == 0) {
                return ZERO
            }

            val gcd: BigInteger = numerator.gcd(denominator)

            var n: BigInteger = numerator.divide(gcd)
            var d: BigInteger = denominator.divide(gcd)

            if (d.signum() < 0) {
                n = n.negate()
                d = d.negate()
            }

            return Rational(
                numerator = n,
                denominator = d,
            )
        }

        fun of(
            value: BigDecimal,
        ): Rational {
            val scale: Int = value.scale()

            return if (scale >= 0) {
                of(
                    numerator = value
                        .unscaledValue(),
                    denominator = BigInteger.TEN
                        .pow(scale),
                )
            } else {
                of(
                    numerator = value
                        .unscaledValue()
                        .multiply(BigInteger.TEN.pow(-scale)),
                    denominator = BigInteger.ONE,
                )
            }
        }
    }

    operator fun plus(
        other: Rational,
    ): Rational {
        val gcd: BigInteger = denominator.gcd(other.denominator)
        val leftMultiplier: BigInteger = other.denominator.divide(gcd)
        val rightMultiplier: BigInteger = denominator.divide(gcd)

        return of(
            numerator = numerator
                .multiply(leftMultiplier)
                .add(other.numerator.multiply(rightMultiplier)),
            denominator = denominator
                .multiply(leftMultiplier),
        )
    }

    operator fun minus(
        other: Rational,
    ): Rational {
        val gcd: BigInteger = denominator.gcd(other.denominator)
        val leftMultiplier: BigInteger = other.denominator.divide(gcd)
        val rightMultiplier: BigInteger = denominator.divide(gcd)

        return of(
            numerator = numerator
                .multiply(leftMultiplier)
                .subtract(other.numerator.multiply(rightMultiplier)),
            denominator = denominator
                .multiply(leftMultiplier),
        )
    }

    operator fun times(
        other: Rational,
    ): Rational {
        val gcd1: BigInteger = numerator.abs().gcd(other.denominator)
        val gcd2: BigInteger = other.numerator.abs().gcd(denominator)

        return of(
            numerator = numerator
                .divide(gcd1)
                .multiply(other.numerator.divide(gcd2)),
            denominator = denominator
                .divide(gcd2)
                .multiply(other.denominator.divide(gcd1)),
        )
    }

    operator fun div(
        other: Rational,
    ): Rational {
        require(!other.isZero())

        val gcd1: BigInteger = numerator.abs().gcd(other.numerator.abs())
        val gcd2: BigInteger = denominator.gcd(other.denominator)

        return of(
            numerator = numerator
                .divide(gcd1)
                .multiply(other.denominator.divide(gcd2)),
            denominator = denominator
                .divide(gcd2)
                .multiply(other.numerator.divide(gcd1)),
        )
    }

    fun signum(): Int =
        numerator.signum()

    fun negate(): Rational =
        of(
            numerator = numerator.negate(),
            denominator = denominator,
        )

    fun isZero(): Boolean =
        numerator.signum() == 0

    fun isInteger(): Boolean =
        denominator == BigInteger.ONE

    fun toBigDecimal(mc: MathContext): BigDecimal {
        return if (isInteger()) {
            numerator.toBigDecimal()
        } else {
            numerator.toBigDecimal()
                .divide(denominator.toBigDecimal(), mc)
        }
    }

    fun toBigIntegerExact(): BigInteger {
        if (!isInteger()) {
            throw ArithmeticException()
        }

        return numerator
    }

    suspend fun pow(exponent: Int): Rational {
        require(exponent != Int.MIN_VALUE)

        val n: Int = abs(exponent)

        val numeratorResult: BigInteger =
            BigDecimalMath.powNaturalExponent(
                base = numerator,
                exponent = n,
            )

        val denominatorResult: BigInteger =
            BigDecimalMath.powNaturalExponent(
                base = denominator,
                exponent = n,
            )

        return if (exponent > 0) {
            of(
                numerator = numeratorResult,
                denominator = denominatorResult,
            )
        } else {
            of(
                numerator = denominatorResult,
                denominator = numeratorResult,
            )
        }
    }
}

package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class DivisionByZeroException : ArithmeticException()

class UnifiedValue private constructor(
    val exact: BigDecimal = BigDecimal.ZERO,
    val rational: Rational = Rational.ZERO,
    val piFactor: Rational = Rational.ZERO,
) {
    companion object {
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
        val NEGATIVE_ONE: UnifiedValue = UnifiedValue(
            rational = Rational.ONE,
        ).negate()
        val ZERO: UnifiedValue = UnifiedValue(
            rational = Rational.ZERO,
        )
        val ONE: UnifiedValue = UnifiedValue(
            rational = Rational.ONE,
        )

        fun fromExactValue(
            exactValue: BigDecimal,
        ): UnifiedValue = UnifiedValue(
            exact = exactValue,
        )

        fun fromExactValue(
            exactValue: BigInteger,
        ): UnifiedValue = UnifiedValue(
            exact = exactValue.toBigDecimal(),
        )

        fun from(
            value: BigDecimal,
        ): UnifiedValue = UnifiedValue(
            rational = Rational.from(value),
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
            value = BigDecimalMath.e(
                mc = mc,
            ),
        )
    }

    fun isPureRational(): Boolean =
        exact.signum() == 0 &&
                piFactor.isZero()

    fun hasPiFactor(): Boolean =
        !piFactor.isZero()

    fun toPiMultipleOrNull(): Rational? {
        return if (exact.signum() == 0 &&
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
        var value: BigDecimal = exact.add(rational.toBigDecimal(mc))

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

    fun toBigIntegerExact(): BigInteger =
        toRationalOrNull()?.toBigIntegerExact()
            ?: throw ArithmeticException()

    suspend fun signum(
        mc: MathContext,
    ): Int =
        toRationalOrNull()?.signum()
            ?: toBigDecimal(mc).signum()

    fun negate(): UnifiedValue = UnifiedValue(
        exact = exact.negate(),
        rational = rational.negate(),
        piFactor = piFactor.negate(),
    )

    fun add(
        other: UnifiedValue,
    ): UnifiedValue = UnifiedValue(
        exact = exact.add(other.exact),
        rational = rational + other.rational,
        piFactor = piFactor + other.piFactor,
    )

    fun subtract(
        other: UnifiedValue,
    ): UnifiedValue = UnifiedValue(
        exact = exact.subtract(other.exact),
        rational = rational - other.rational,
        piFactor = piFactor - other.piFactor,
    )

    suspend fun multiply(
        other: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        val leftExactRational: Rational =
            Rational.from(exact)
        val rightExactRational: Rational =
            Rational.from(other.exact)

        var newExact: BigDecimal =
            exact.multiply(other.exact)

        if (!piFactor.isZero() && !other.piFactor.isZero()) {
            val workingMc: MathContext = workingMc(mc)

            newExact = newExact.add(
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

        val newRational: Rational =
            rational * other.rational +
                    leftExactRational * other.rational +
                    rightExactRational * rational

        val newPiFactor: Rational =
            (leftExactRational + rational) * other.piFactor +
                    (rightExactRational + other.rational) * piFactor

        return UnifiedValue(
            exact = newExact,
            rational = newRational,
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
                rational = (Rational.from(exact) + rational) / denominator,
                piFactor = piFactor / denominator,
            )
        } else {
            val workingMc: MathContext = workingMc(mc)

            val approximateDenominator: BigDecimal = other.toBigDecimal(workingMc)
            if (approximateDenominator.signum() == 0) {
                throw DivisionByZeroException()
            }

            UnifiedValue(
                exact = toBigDecimal(workingMc)
                    .divide(approximateDenominator, mc)
            )
        }
    }

    private fun toRationalOrNull(): Rational? {
        return if (isPureRational()) {
            rational
        } else if (!hasPiFactor()) {
            rational + Rational.from(exact)
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

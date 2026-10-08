package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class FactorialLargeArgumentException : ArithmeticException()
class FactorialInvalidArgumentException : ArithmeticException()
class SqrtInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class LogInvalidArgumentException : ArithmeticException()
class TanInvalidArgumentException : ArithmeticException()

class BigDecimalMath(private val mathContext: MathContext) {
    companion object {
        private const val MAX_FACTORIAL_ARGUMENT = 100
        private const val EXTRA_PRECISION = 8
        private val ZERO = BigDecimal.ZERO
        private val ONE = BigDecimal.ONE
        private val TWO = BigDecimal(2)
        private val FOUR = BigDecimal(4)
        private val FIVE = BigDecimal(5)
        private val TEN = BigDecimal.TEN
        private val SIXTEEN = BigDecimal(16)
        private val TWO_HUNDRED_THIRTY_NINE = BigDecimal(239)
    }

    private val workMathContext: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION,
        mathContext.roundingMode,
    )
    private val sqrtTwo: BigDecimal by lazy {
        sqrt(TWO, workMathContext)
    }
    private val inverseSqrtTwo: BigDecimal by lazy {
        ONE.divide(sqrtTwo, workMathContext)
    }
    private val lnTwo: BigDecimal by lazy {
        calculateLn(TWO, workMathContext)
    }
    private val lnTen: BigDecimal by lazy {
        ln(TEN, workMathContext)
    }
    private val pi: BigDecimal by lazy {
        pi(workMathContext)
    }
    private val halfPi: BigDecimal by lazy {
        pi.divide(TWO, workMathContext)
    }
    private val oneAndHalfPi: BigDecimal by lazy {
        pi.add(halfPi, workMathContext)
    }
    private val twoPi: BigDecimal by lazy {
        pi.multiply(TWO, workMathContext)
    }

    fun e(): BigDecimal {
        var sum: BigDecimal = ONE
        var factorial: BigDecimal = ONE
        var n = 1

        while (true) {
            factorial = factorial.multiply(
                BigDecimal(n),
                workMathContext,
            )

            val term: BigDecimal = ONE.divide(
                factorial,
                workMathContext,
            )
            val next: BigDecimal = sum.add(
                term,
                workMathContext,
            )

            if (next.compareTo(sum) == 0) {
                return next.round(mathContext)
            }

            sum = next
            n++
        }
    }

    fun pi(): BigDecimal {
        return pi(workMathContext).round(mathContext)
    }

    fun factorial(value: BigDecimal): BigDecimal {
        val integerValue: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue < BigInteger.ZERO) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue > MAX_FACTORIAL_ARGUMENT.toBigInteger()) {
            throw FactorialLargeArgumentException()
        }

        var result: BigInteger = BigInteger.ONE
        var i: BigInteger = BigInteger.ONE

        while (i <= integerValue) {
            result = result.multiply(i)
            i = i.add(BigInteger.ONE)
        }

        return BigDecimal(result)
    }

    fun sqrt(value: BigDecimal): BigDecimal {
        return sqrt(value, workMathContext).round(mathContext)
    }

    fun pow(
        base: BigDecimal,
        exponent: BigDecimal,
    ): BigDecimal {
        return if (base <= ZERO) {
            powNegativeBase(base, exponent)
        } else {
            val value: BigDecimal = exponent.multiply(
                ln(base, workMathContext),
                workMathContext,
            )

            exp(
                value,
                workMathContext,
            ).round(mathContext)
        }
    }

    fun ln(value: BigDecimal): BigDecimal {
        return ln(value, workMathContext).round(mathContext)
    }

    fun log(value: BigDecimal): BigDecimal {
        return ln(
            value,
            workMathContext,
        ).divide(
            lnTen,
            workMathContext,
        ).round(mathContext)
    }

    fun sin(value: BigDecimal): BigDecimal {
        return sin(value, workMathContext).round(mathContext)
    }

    fun cos(value: BigDecimal): BigDecimal {
        return cos(value, workMathContext).round(mathContext)
    }

    fun tan(value: BigDecimal): BigDecimal {
        val cos: BigDecimal = cos(value, workMathContext)
        if (cos.round(mathContext).compareTo(ZERO) == 0) {
            throw TanInvalidArgumentException()
        }

        val sin: BigDecimal = sin(value, workMathContext)

        return sin.divide(
            cos,
            workMathContext,
        ).round(mathContext)
    }

    private fun pi(mc: MathContext): BigDecimal {
        val arctanOneFifth: BigDecimal =
            arctan(
                ONE.divide(FIVE, mc),
                mc,
            )
        val arctanOneTwoHundredThirtyNine: BigDecimal =
            arctan(
                ONE.divide(TWO_HUNDRED_THIRTY_NINE, mc),
                mc,
            )

        return SIXTEEN
            .multiply(
                arctanOneFifth,
                mc,
            )
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine,
                    mc,
                ),
                mc,
            )
    }

    private fun arctan(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val valueSquared: BigDecimal = value.multiply(value, mc)

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var denominator = ONE
        var sign: Int = -1

        while (true) {
            term = term.multiply(valueSquared, mc)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(
                denominator,
                mc,
            )

            val nextSum: BigDecimal = if (sign > 0) {
                sum.add(
                    nextTerm,
                    mc,
                )
            } else {
                sum.subtract(
                    nextTerm,
                    mc,
                )
            }

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            sign = -sign
        }
    }

    private fun sqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        fun initialSqrtEstimate(value: BigDecimal): BigDecimal {
            val integerDigits: Int = value.precision() - value.scale()
            val exponent: Int = integerDigits / 2

            return TEN.pow(exponent.coerceAtLeast(0))
        }

        if (value < ZERO) {
            throw SqrtInvalidArgumentException()
        }

        if (value.compareTo(ZERO) == 0) {
            return ZERO
        }

        var x: BigDecimal = initialSqrtEstimate(value)

        while (true) {
            val next: BigDecimal = (
                    x + value.divide(
                        x,
                        mc,
                    )).divide(
                TWO,
                mc,
            )
            if (next.compareTo(x) == 0) {
                return next
            }

            x = next
        }
    }

    private fun powNegativeBase(
        base: BigDecimal,
        exponent: BigDecimal,
    ): BigDecimal {
        if ((base == ZERO) && (exponent <= ZERO)) {
            throw PowInvalidArgumentException()
        }

        val integerExponent: BigInteger = try {
            exponent.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw PowInvalidArgumentException()
        }

        return base.pow(
            integerExponent.intValueExact(),
            workMathContext
        ).round(mathContext)
    }

    private fun exp(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        var sum: BigDecimal = ONE
        var term: BigDecimal = ONE
        var n = 1

        while (true) {
            term = term
                .multiply(value, mc)
                .divide(
                    BigDecimal(n),
                    mc,
                )

            val nextSum: BigDecimal = sum.add(
                term,
                mc,
            )

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private fun ln(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value <= ZERO) {
            throw LogInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        var x: BigDecimal = value
        var exponent = 0

        while (x > sqrtTwo) {
            x = x.divide(
                TWO,
                mc,
            )
            exponent++
        }

        while (x < inverseSqrtTwo) {
            x = x.multiply(
                TWO,
                mc,
            )
            exponent--
        }

        val lnX: BigDecimal = calculateLn(x, mc)

        return lnX
            .add(
                BigDecimal(exponent).multiply(
                    lnTwo,
                    mc,
                ),
                mc,
            )
    }

    private fun calculateLn(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val z: BigDecimal = value.subtract(ONE, mc)
            .divide(
                value.add(ONE, mc),
                mc,
            )
        val zSquared: BigDecimal = z.multiply(
            z,
            mc,
        )

        var term: BigDecimal = z
        var sum: BigDecimal = z
        var denominator: BigDecimal = ONE

        while (true) {
            term = term.multiply(
                zSquared,
                mc
            )
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(
                denominator,
                mc,
            )
            val nextSum = sum.add(
                nextTerm,
                mc,
            )

            if (nextSum.compareTo(sum) == 0) {
                return nextSum.multiply(
                    TWO,
                    mc,
                )
            }

            sum = nextSum
        }
    }

    private fun sin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val angle: BigDecimal = normalizeAngle(value, mc)

        return when {
            angle <= halfPi -> {
                sinTaylor(
                    angle,
                    mc,
                )
            }

            angle <= pi -> {
                sinTaylor(
                    pi.subtract(
                        angle,
                        mc,
                    ),
                    mc,
                )
            }

            angle <= oneAndHalfPi -> {
                sinTaylor(
                    angle.subtract(
                        pi,
                        mc,
                    ),
                    mc,
                ).negate(mc)
            }

            else -> {
                sinTaylor(
                    twoPi.subtract(
                        angle,
                        mc,
                    ),
                    mc,
                ).negate(mc)
            }
        }
    }

    private fun cos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val angle: BigDecimal = normalizeAngle(value, mc)

        return when {
            angle <= halfPi -> {
                cosTaylor(
                    angle,
                    mc,
                )
            }

            angle <= pi -> {
                cosTaylor(
                    pi.subtract(
                        angle,
                        mc,
                    ),
                    mc,
                ).negate()
            }

            angle <= oneAndHalfPi -> {
                cosTaylor(
                    angle.subtract(
                        pi,
                        mc,
                    ),
                    mc,
                ).negate(mc)
            }

            else -> {
                cosTaylor(
                    twoPi.subtract(
                        angle,
                        mc,
                    ),
                    mc,
                )
            }
        }
    }

    private fun sinTaylor(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val valueSquared: BigDecimal = value.multiply(
            value,
            mc,
        )

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var n = 1

        while (true) {
            val denominator = BigDecimal(
                (2 * n) * (2 * n + 1)
            )

            term = term
                .multiply(valueSquared, mc)
                .divide(denominator, mc)
                .negate()

            val nextSum: BigDecimal = sum.add(
                term,
                mc,
            )

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private fun cosTaylor(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val valueSquared: BigDecimal = value.multiply(
            value,
            mc,
        )

        var term: BigDecimal = ONE
        var sum: BigDecimal = ONE
        var n = 1

        while (true) {
            val denominator = BigDecimal(
                (2 * n - 1) * (2 * n)
            )

            term = term
                .multiply(valueSquared, mc)
                .divide(denominator, mc)
                .negate()

            val nextSum: BigDecimal = sum.add(
                term,
                mc,
            )

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private fun normalizeAngle(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        var angle: BigDecimal = value.remainder(twoPi, mc)

        if (angle < ZERO) {
            angle = angle.add(twoPi, mc)
        }

        return angle
    }
}

package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class LargeArgumentException : ArithmeticException()
class FactorialNonPositiveIntegerException : ArithmeticException()
class SqrtNegativeArgumentException : ArithmeticException()

class BigDecimalMath(private val mathContext: MathContext) {
    companion object {
        private const val MAX_FACTORIAL_ARGUMENT = 100
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
        mathContext.precision + 8,
        mathContext.roundingMode,
    )

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
        val arctanOneFifth: BigDecimal =
            arctan(
                ONE.divide(FIVE, workMathContext),
                workMathContext,
            )
        val arctanOneTwoHundredThirtyNine: BigDecimal =
            arctan(
                ONE.divide(TWO_HUNDRED_THIRTY_NINE, workMathContext),
                workMathContext,
            )

        return SIXTEEN
            .multiply(
                arctanOneFifth,
                workMathContext,
            )
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine,
                    workMathContext,
                ),
                workMathContext,
            )
            .round(mathContext)
    }

    fun factorial(value: BigDecimal): BigDecimal {
        val integerValue: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw FactorialNonPositiveIntegerException()
        }

        if (integerValue < BigInteger.ZERO) {
            throw FactorialNonPositiveIntegerException()
        }

        if (integerValue > MAX_FACTORIAL_ARGUMENT.toBigInteger()) {
            throw LargeArgumentException()
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
        fun initialSqrtEstimate(value: BigDecimal): BigDecimal {
            val integerDigits: Int = value.precision() - value.scale()
            val exponent: Int = integerDigits / 2

            return TEN.pow(exponent.coerceAtLeast(0))
        }

        if (value < ZERO) {
            throw SqrtNegativeArgumentException()
        }

        if (value.compareTo(ZERO) == 0) {
            return ZERO
        }

        var x: BigDecimal = initialSqrtEstimate(value)

        while (true) {
            val next = (
                    x + value.divide(x, workMathContext)
                    ).divide(TWO, workMathContext)
            if (next.compareTo(x) == 0) {
                return next.round(mathContext)
            }

            x = next
        }
    }

    private fun arctan(value: BigDecimal, mc: MathContext): BigDecimal {
        val valueSquared: BigDecimal = value.multiply(value, mc)

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var denominator = ONE
        var sign: Int = -1

        while (true) {
            term = term.multiply(valueSquared, mc)
            denominator = denominator.add(TWO)

            val nextTerm = term.divide(denominator, mc)

            val nextSum = if (sign > 0) {
                sum.add(nextTerm, mc)
            } else {
                sum.subtract(nextTerm, mc)
            }

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            sign = -sign
        }
    }
}

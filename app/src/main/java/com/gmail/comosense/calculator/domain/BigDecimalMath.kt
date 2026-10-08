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
    }

    fun e(): BigDecimal {
        val mc = MathContext(
            mathContext.precision + 2,
            mathContext.roundingMode,
        )
        var sum: BigDecimal = BigDecimal.ONE
        var factorial: BigDecimal = BigDecimal.ONE
        var n = 1

        while (true) {
            factorial = factorial.multiply(BigDecimal(n), mc)

            val term: BigDecimal = BigDecimal.ONE.divide(factorial, mc)
            val next: BigDecimal = sum.add(term, mc)

            if (next.compareTo(sum) == 0) {
                return next.round(mathContext)
            }

            sum = next
            n++
        }
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

        return BigDecimal(result).round(mathContext)
    }

    fun sqrt(value: BigDecimal): BigDecimal {
        fun initialSqrtEstimate(value: BigDecimal): BigDecimal {
            val integerDigits: Int = value.precision() - value.scale()
            val exponent: Int = integerDigits / 2

            return BigDecimal.TEN.pow(exponent.coerceAtLeast(0))
        }

        val two = BigDecimal(2)

        if (value < BigDecimal.ZERO) {
            throw SqrtNegativeArgumentException()
        }

        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }

        var x: BigDecimal = initialSqrtEstimate(value)

        while (true) {
            val next = (
                    x + value.divide(x, mathContext)
                    ).divide(two, mathContext)
            if (next.compareTo(x) == 0) {
                return next
            }

            x = next
        }
    }
}

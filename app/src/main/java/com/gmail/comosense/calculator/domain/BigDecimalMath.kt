package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.coroutines.CoroutineContext

internal object BigDecimalMathConstraints {
    const val MAX_FACTORIAL_ARGUMENT: Int = Constraints.MAX_FACTORIAL_ARGUMENT
    const val MAX_INTEGER_EXPONENT: Int = Constraints.MAX_INTEGER_EXPONENT
    const val MAX_NON_INTEGER_EXPONENT: Int = Constraints.MAX_NON_INTEGER_EXPONENT
}

class FactorialLargeArgumentException : ArithmeticException()
class FactorialInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : ArithmeticException()
class SqrtInvalidArgumentException : ArithmeticException()
class LogInvalidArgumentException : ArithmeticException()
class TanInvalidArgumentException : ArithmeticException()
class ArcsinInvalidArgumentException : ArithmeticException()
class ArccosInvalidArgumentException : ArithmeticException()

class BigDecimalMath {
    companion object {
        private const val EXTRA_PRECISION_FOR_INTERNAL_CALCULATIONS: Int = 8
        private const val EXTRA_PRECISION_FOR_ROUND: Int = 1
        private const val EXTRA_PRECISION_FOR_CACHE: Int = 8
        private val NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
        private val ZERO: BigDecimal = BigDecimal.ZERO
        private val ONE_HALF: BigDecimal = BigDecimal("0.5")
        private val ONE: BigDecimal = BigDecimal.ONE
        private val TWO: BigDecimal = BigDecimal(2)
        private val FOUR: BigDecimal = BigDecimal(4)
        private val FIVE: BigDecimal = BigDecimal(5)
        private val TEN: BigDecimal = BigDecimal.TEN
        private val SIXTEEN: BigDecimal = BigDecimal(16)
        private val ONE_HUNDRED_EIGHTY: BigDecimal = BigDecimal(180)
        private val TWO_HUNDRED_THIRTY_NINE: BigDecimal = BigDecimal(239)
        private val BIG_INTEGER_ONE: BigInteger = BigInteger.ONE
        private val BIG_INTEGER_TWO: BigInteger = 2.toBigInteger()
        private val MAX_FACTORIAL_ARGUMENT: BigInteger =
            BigDecimalMathConstraints.MAX_FACTORIAL_ARGUMENT.toBigInteger()
        private val MAX_INTEGER_EXPONENT: BigInteger =
            BigDecimalMathConstraints.MAX_INTEGER_EXPONENT.toBigInteger()
        private val MAX_NON_INTEGER_EXPONENT: BigDecimal =
            BigDecimal(BigDecimalMathConstraints.MAX_NON_INTEGER_EXPONENT)
    }

    private data class MathConstant(
        val mc: MathContext,
        val value: BigDecimal,
    )

    @Volatile
    private var cachedE: MathConstant? = null

    @Volatile
    private var cachedPi: MathConstant? = null

    @Volatile
    private var cachedLnTen: MathConstant? = null

    @Volatile
    private var cachedSqrtTwo: MathConstant? = null

    @Volatile
    private var cachedInverseSqrtTwo: MathConstant? = null

    @Volatile
    private var cachedLnTwo: MathConstant? = null

    @Volatile
    private var cachedHalfPi: MathConstant? = null

    @Volatile
    private var cachedThreeHalfPi: MathConstant? = null

    @Volatile
    private var cachedTwoPi: MathConstant? = null

    suspend fun e(
        mc: MathContext,
    ): BigDecimal {
        return cachedE(
            mc = internalMc(mc),
        ).round(mc)
    }

    suspend fun pi(
        mc: MathContext,
    ): BigDecimal {
        return cachedPi(
            mc = internalMc(mc),
        ).round(mc)
    }

    suspend fun factorial(
        value: BigDecimal,
    ): BigDecimal {
        val integerValue: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue.signum() < 0) {
            throw FactorialInvalidArgumentException()
        }

        if (integerValue > MAX_FACTORIAL_ARGUMENT) {
            throw FactorialLargeArgumentException()
        }

        return calculateFactorial(integerValue)
    }

    suspend fun sqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateSqrt(
            value = value,
            mc = internalMc(mc),
        ).round(mc)
    }

    suspend fun pow(
        base: BigDecimal,
        exponent: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (base.signum() == 0) {
            return if (exponent.signum() > 0) {
                ZERO
            } else {
                throw PowInvalidArgumentException()
            }
        }

        if (exponent.signum() == 0) {
            return ONE
        }

        val integerExponent: BigInteger? = try {
            exponent.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }

        if (integerExponent != null) {
            if (integerExponent.abs() > MAX_INTEGER_EXPONENT) {
                throw PowLimitExceededException()
            }
            val result = calculateIntegerPow(
                base = base,
                exponent = integerExponent.abs().intValueExact(),
            )

            return if (integerExponent.signum() > 0) {
                result
            } else {
                ONE.divide(result, mc)
            }
        } else {
            if (exponent.abs() > MAX_NON_INTEGER_EXPONENT) {
                throw PowLimitExceededException()
            }
            if (base.signum() < 0) {
                throw PowInvalidArgumentException()
            }

            val internalMc: MathContext = internalMc(mc)
            return calculateExp(
                value = exponent.multiply(
                    computeLn(
                        value = base,
                        mc = internalMc,
                    )
                ),
                mc = internalMc,
            ).round(mc)
        }
    }

    suspend fun ln(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return computeLn(
            value = value,
            mc = internalMc(mc),
        ).round(mc)
    }

    suspend fun log(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        return computeLn(value, internalMc)
            .divide(cachedLnTen(mc), mc)
    }

    suspend fun sin(
        angle: BigDecimal,
        isRadians: Boolean,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        return calculateSin(
            angle = if (isRadians) {
                angle
            } else {
                angle.toRadians(internalMc)
            }.normalizedAngle(internalMc),
            tolerance = tolerance,
            mc = internalMc,
        ).round(mc)
    }

    suspend fun cos(
        angle: BigDecimal,
        isRadians: Boolean,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        return calculateCos(
            angle = if (isRadians) {
                angle
            } else {
                angle.toRadians(internalMc)
            }.normalizedAngle(internalMc),
            tolerance = tolerance,
            mc = internalMc,
        ).round(mc)
    }

    suspend fun tan(
        angle: BigDecimal,
        isRadians: Boolean,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        return calculateTan(
            angle = if (isRadians) {
                angle
            } else {
                angle.toRadians(internalMc)
            }.normalizedAngle(internalMc),
            tolerance = tolerance,
            mc = internalMc,
        ).round(mc)
    }

    suspend fun arcsin(
        value: BigDecimal,
        isRadians: Boolean,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        val result: BigDecimal = calculateArcsin(
            value = value,
            mc = internalMc,
        )

        return if (isRadians) {
            result
        } else {
            result.toDegrees(internalMc)
        }.round(mc)
    }

    suspend fun arccos(
        value: BigDecimal,
        isRadians: Boolean,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        val result: BigDecimal = calculateArccos(
            value = value,
            mc = internalMc,
        )

        return if (isRadians) {
            result
        } else {
            result.toDegrees(internalMc)
        }.round(mc)
    }

    suspend fun arctan(
        value: BigDecimal,
        isRadians: Boolean,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = internalMc(mc)

        val result: BigDecimal = calculateArctan(
            value = value,
            mc = internalMc,
        )

        return if (isRadians) {
            result
        } else {
            result.toDegrees(internalMc)
        }.round(mc)
    }

    private suspend fun computeLn(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        if (value.signum() <= 0) {
            throw LogInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        val sqrtTwo: BigDecimal = cachedSqrtTwo(mc)
        val inverseSqrtTwo: BigDecimal = cachedInverseSqrtTwo(mc)
        val lnTwo: BigDecimal = cachedLnTwo(mc)

        var x: BigDecimal = value
        var exponent = 0

        while (x > sqrtTwo) {
            coroutineContext.ensureActive()

            x = x.divide(TWO, mc)
            exponent++
        }

        while (x < inverseSqrtTwo) {
            coroutineContext.ensureActive()

            x = x.multiply(TWO)
            exponent--
        }

        val lnX: BigDecimal = calculateLn(
            value = x,
            mc = mc,
        )

        return lnX.add(
            BigDecimal(exponent)
                .multiply(lnTwo)
        )
    }

    private suspend fun calculateE(
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var sum: BigDecimal = ONE
        var term: BigDecimal = ONE
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            term = term.divide(BigDecimal(n), mc)
            val next: BigDecimal = sum.add(term).round(mc)

            if (next.compareTo(sum) == 0) {
                return next
            }

            sum = next
            n++
        }
    }

    private suspend fun calculatePi(
        mc: MathContext,
    ): BigDecimal {
        val arctanOneFifth: BigDecimal = arctanTaylor(
            value = ONE.divide(FIVE, mc),
            mc = mc,
        )
        val arctanOneTwoHundredThirtyNine: BigDecimal = arctanTaylor(
            value = ONE.divide(TWO_HUNDRED_THIRTY_NINE, mc),
            mc = mc,
        )

        return SIXTEEN
            .multiply(arctanOneFifth)
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine
                )
            )
    }

    private suspend fun calculateFactorial(
        value: BigInteger,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var result: BigInteger = BigInteger.ONE
        var i: BigInteger = BigInteger.ONE

        while (i <= value) {
            coroutineContext.ensureActive()

            result = result.multiply(i)
            i = i.add(BigInteger.ONE)
        }

        return BigDecimal(result)
    }

    private suspend fun calculateSqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        fun initialSqrtEstimate(v: BigDecimal): BigDecimal {
            val orderOfMagnitude: Int = v.precision() - v.scale() - 1
            val sqrtExponent: Int = Math.floorDiv(orderOfMagnitude, 2)

            return ONE.scaleByPowerOfTen(sqrtExponent)
        }

        if (value.signum() < 0) {
            throw SqrtInvalidArgumentException()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        var x: BigDecimal = initialSqrtEstimate(value)

        var previousX: BigDecimal? = null
        while (true) {
            coroutineContext.ensureActive()

            val next: BigDecimal = x
                .add(value.divide(x, mc))
                .divide(TWO, mc)
            if (next.compareTo(x) == 0 ||
                (previousX != null && next.compareTo(previousX) == 0)
            ) {
                return minOf(x, next)
            }
            previousX = x
            x = next
        }
    }

    private suspend fun calculateIntegerPow(
        base: BigDecimal,
        exponent: Int,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var n: Long = exponent.toLong()
        var currentBase: BigDecimal = base
        var result: BigDecimal = ONE

        while (n > 0L) {
            coroutineContext.ensureActive()

            if ((n and 1L) != 0L) {
                result = result.multiply(currentBase)
            }

            n = n shr 1

            if (n > 0L) {
                currentBase = currentBase.multiply(currentBase)
            }
        }

        return result
    }

    private suspend fun calculateExp(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var reduced: BigDecimal = value
        var reductions = 0

        while (reduced.abs() > ONE_HALF) {
            coroutineContext.ensureActive()

            reduced = reduced.divide(TWO)
            reductions++
        }

        val extendedMc = MathContext(
            mc.precision + reductions,
            mc.roundingMode,
        )

        var sum: BigDecimal = ONE
        var term: BigDecimal = ONE
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            term = term
                .multiply(reduced)
                .divide(BigDecimal(n), extendedMc)

            val nextSum: BigDecimal = sum.add(term).round(extendedMc)

            if (nextSum.compareTo(sum) == 0) {
                sum = nextSum
                break
            }

            sum = nextSum
            n++
        }

        repeat(reductions) {
            coroutineContext.ensureActive()

            sum = sum.multiply(sum)
        }

        return sum
    }

    private suspend fun calculateLn(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val z: BigDecimal = value
            .subtract(ONE)
            .divide(value.add(ONE), mc)
        val zSquared: BigDecimal = z.multiply(z)

        var term: BigDecimal = z
        var sum: BigDecimal = z
        var denominator: BigDecimal = ONE

        while (true) {
            coroutineContext.ensureActive()

            term = term.multiply(zSquared)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(denominator, mc)
            val nextSum: BigDecimal = sum.add(nextTerm).round(mc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum.multiply(TWO)
            }

            sum = nextSum
        }
    }

    private suspend fun calculateSin(
        angle: BigDecimal,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val pi: BigDecimal = cachedPi(mc)
        val halfPi: BigDecimal = cachedHalfPi(mc)
        val threeHalfPi: BigDecimal = cachedThreeHalfPi(mc)
        val twoPi: BigDecimal = cachedTwoPi(mc)

        val distanceToZero: BigDecimal =
            minOf(angle, twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(threeHalfPi).abs()

        val useTolerance: Boolean = tolerance.signum() > 0
        return when {
            useTolerance &&
                    (distanceToZero <= tolerance || distanceToPi <= tolerance) ->
                ZERO

            useTolerance &&
                    distanceToHalfPi <= tolerance ->
                ONE

            useTolerance &&
                    distanceToThreeHalfPi <= tolerance ->
                NEGATIVE_ONE

            angle <= halfPi ->
                sinTaylor(
                    angle = angle,
                    mc = mc,
                )

            angle <= pi ->
                sinTaylor(
                    angle = pi.subtract(angle),
                    mc = mc,
                )

            angle <= threeHalfPi ->
                sinTaylor(
                    angle = angle.subtract(pi),
                    mc = mc,
                ).negate()

            else ->
                sinTaylor(
                    angle = twoPi.subtract(angle),
                    mc = mc,
                ).negate()
        }
    }

    private suspend fun calculateCos(
        angle: BigDecimal,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val pi: BigDecimal = cachedPi(mc)
        val halfPi: BigDecimal = cachedHalfPi(mc)
        val threeHalfPi: BigDecimal = cachedThreeHalfPi(mc)
        val twoPi: BigDecimal = cachedTwoPi(mc)

        val distanceToZero: BigDecimal =
            minOf(angle, twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(threeHalfPi).abs()

        val useTolerance: Boolean = tolerance.signum() > 0
        return when {
            useTolerance &&
                    (distanceToHalfPi <= tolerance || distanceToThreeHalfPi <= tolerance) ->
                ZERO

            useTolerance &&
                    distanceToZero <= tolerance ->
                ONE

            useTolerance &&
                    distanceToPi <= tolerance ->
                NEGATIVE_ONE

            angle <= halfPi ->
                cosTaylor(
                    angle = angle,
                    mc = mc,
                )

            angle <= pi ->
                cosTaylor(
                    angle = pi.subtract(angle),
                    mc = mc,
                ).negate()

            angle <= threeHalfPi ->
                cosTaylor(
                    angle = angle.subtract(pi),
                    mc = mc,
                ).negate()

            else ->
                cosTaylor(
                    angle = twoPi.subtract(angle),
                    mc = mc,
                )
        }
    }

    private suspend fun calculateTan(
        angle: BigDecimal,
        tolerance: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val cos: BigDecimal = calculateCos(
            angle = angle,
            tolerance = tolerance,
            mc = mc,
        )

        if (cos.signum() == 0) {
            throw TanInvalidArgumentException()
        }

        val sin: BigDecimal = calculateSin(
            angle = angle,
            tolerance = tolerance,
            mc = mc,
        )

        return sin.divide(cos, mc)
    }

    private suspend fun calculateArcsin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value !in NEGATIVE_ONE..ONE) {
            throw ArcsinInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return cachedPi(mc).divide(TWO, mc)
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return cachedPi(mc).divide(TWO, mc).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val absValue: BigDecimal = value.abs()
        val oneMinusXSquared: BigDecimal = ONE
            .subtract(absValue)
            .multiply(ONE.add(absValue))
        val root: BigDecimal = calculateSqrt(oneMinusXSquared, mc)

        return calculateArctan(
            value = value.divide(root, mc),
            mc = mc,
        )
    }

    private suspend fun calculateArccos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value !in NEGATIVE_ONE..ONE) {
            throw ArccosInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return cachedPi(mc)
        }

        if (value.signum() == 0) {
            return cachedPi(mc).divide(TWO, mc)
        }

        if (value.signum() < 0) {
            return cachedPi(mc).subtract(
                calculateArccos(
                    value = value.negate(),
                    mc = mc,
                )
            )
        }

        val numerator: BigDecimal = ONE.subtract(value)
        val denominator: BigDecimal = ONE.add(value)
        val ratio: BigDecimal = numerator.divide(denominator, mc)
        val root: BigDecimal = calculateSqrt(ratio, mc)

        return TWO.multiply(
            calculateArctan(
                value = root,
                mc = mc,
            )
        )
    }

    private suspend fun calculateArctan(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() < 0) {
            return calculateArctan(
                value = value.negate(),
                mc = mc,
            ).negate()
        }

        if (value > ONE) {
            return cachedPi(mc).divide(TWO, mc)
                .subtract(
                    calculateArctan(
                        value = ONE.divide(value, mc),
                        mc = mc,
                    )
                )
        }

        if (value > ONE_HALF) {
            val reduced: BigDecimal = value.subtract(ONE)
                .divide(value.add(ONE), mc)

            return cachedPi(mc).divide(FOUR, mc)
                .add(
                    calculateArctan(
                        value = reduced,
                        mc = mc,
                    )
                )
        }

        return arctanTaylor(
            value = value,
            mc = mc,
        )
    }

    private suspend fun sinTaylor(
        angle: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val angleSquared: BigDecimal = angle.multiply(angle)

        var term: BigDecimal = angle
        var sum: BigDecimal = angle
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            val twoN: BigInteger = BIG_INTEGER_TWO
                .multiply(n.toBigInteger())
            val denominator: BigInteger = twoN
                .multiply(twoN.add(BIG_INTEGER_ONE))

            term = term
                .multiply(angleSquared)
                .divide(BigDecimal(denominator), mc)
                .negate()

            val nextSum: BigDecimal = sum.add(term).round(mc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private suspend fun cosTaylor(
        angle: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val angleSquared: BigDecimal = angle.multiply(angle)

        var term: BigDecimal = ONE
        var sum: BigDecimal = ONE
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            val twoN: BigInteger = BIG_INTEGER_TWO
                .multiply(n.toBigInteger())
            val denominator: BigInteger = twoN
                .subtract(BIG_INTEGER_ONE)
                .multiply(twoN)

            term = term
                .multiply(angleSquared)
                .divide(BigDecimal(denominator), mc)
                .negate()

            val nextSum: BigDecimal = sum.add(term).round(mc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private suspend fun arctanTaylor(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val valueSquared: BigDecimal = value.multiply(value)

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var denominator: BigDecimal = ONE
        var sign: Int = -1

        while (true) {
            coroutineContext.ensureActive()

            term = term.multiply(valueSquared)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(denominator, mc)

            val nextSum: BigDecimal = if (sign > 0) {
                sum.add(nextTerm)
            } else {
                sum.subtract(nextTerm)
            }.round(mc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            sign = -sign
        }
    }

    private suspend fun BigDecimal.toRadians(
        mc: MathContext
    ): BigDecimal {
        return this
            .multiply(cachedPi(mc))
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }

    private suspend fun BigDecimal.toDegrees(
        mc: MathContext,
    ): BigDecimal {
        return this
            .multiply(ONE_HUNDRED_EIGHTY)
            .divide(cachedPi(mc), mc)
    }

    private suspend fun BigDecimal.normalizedAngle(
        mc: MathContext
    ): BigDecimal {
        val twoPi: BigDecimal = cachedTwoPi(mc)

        var angle: BigDecimal = this.remainder(twoPi)

        if (angle.signum() < 0) {
            angle = angle.add(twoPi)
        }

        return angle
    }

    private suspend fun cachedLnTen(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedLnTen,
        calculate = { mathContext ->
            computeLn(
                value = TEN,
                mc = mathContext,
            )
        },
        updateCache = { cached -> cachedLnTen = cached },
        mc = mc,
    )

    private suspend fun cachedSqrtTwo(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedSqrtTwo,
        calculate = { mathContext ->
            calculateSqrt(
                value = TWO,
                mc = mathContext,
            )
        },
        updateCache = { cached -> cachedSqrtTwo = cached },
        mc = mc,
    )

    private suspend fun cachedInverseSqrtTwo(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedInverseSqrtTwo,
        calculate = { mathContext ->
            ONE.divide(
                cachedSqrtTwo(
                    mc = mathContext,
                ), mathContext
            )
        },
        updateCache = { cached -> cachedInverseSqrtTwo = cached },
        mc = mc,
    )

    private suspend fun cachedLnTwo(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedLnTwo,
        calculate = { mathContext ->
            calculateLn(
                value = TWO,
                mc = mathContext,
            )
        },
        updateCache = { cached -> cachedLnTwo = cached },
        mc = mc,
    )

    private suspend fun cachedE(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedE,
        calculate = { mathContext ->
            calculateE(
                mc = mathContext,
            )
        },
        updateCache = { cached -> cachedE = cached },
        mc = mc,
    )

    private suspend fun cachedPi(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedPi,
        calculate = { mathContext ->
            calculatePi(
                mc = mathContext,
            )
        },
        updateCache = { cached -> cachedPi = cached },
        mc = mc,
    )

    private suspend fun cachedHalfPi(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedHalfPi,
        calculate = { mathContext ->
            cachedPi(
                mc = mathContext,
            ).divide(TWO)
        },
        updateCache = { cached -> cachedHalfPi = cached },
        mc = mc,
    )

    private suspend fun cachedThreeHalfPi(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedThreeHalfPi,
        calculate = { mathContext ->
            cachedPi(
                mc = mathContext,
            ).add(cachedHalfPi(mathContext))
        },
        updateCache = { cached -> cachedThreeHalfPi = cached },
        mc = mc,
    )

    private suspend fun cachedTwoPi(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedTwoPi,
        calculate = { mathContext ->
            cachedPi(
                mc = mathContext,
            ).multiply(TWO)
        },
        updateCache = { cached -> cachedTwoPi = cached },
        mc = mc,
    )

    private suspend fun cachedConstant(
        cachedValue: MathConstant?,
        calculate: suspend (MathContext) -> BigDecimal,
        updateCache: (MathConstant) -> Unit,
        mc: MathContext,
    ): BigDecimal {
        cachedValue?.let { cached ->
            if (cached.mc.precision >= (mc.precision + EXTRA_PRECISION_FOR_ROUND) ||
                cached.mc == mc
            ) {
                return cached.value.round(mc)
            }
        }

        val mathContext = MathContext(
            mc.precision + EXTRA_PRECISION_FOR_CACHE,
            mc.roundingMode
        )
        val value = calculate(mathContext)

        updateCache(
            MathConstant(
                mc = mathContext,
                value = value,
            )
        )

        return value.round(mc)
    }

    private fun internalMc(mc: MathContext): MathContext {
        return MathContext(
            mc.precision + EXTRA_PRECISION_FOR_INTERNAL_CALCULATIONS,
            mc.roundingMode,
        )
    }
}

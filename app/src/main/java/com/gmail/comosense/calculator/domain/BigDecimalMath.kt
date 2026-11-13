package com.gmail.comosense.calculator.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.coroutines.CoroutineContext

class SqrtInvalidArgumentException : ArithmeticException()
class LogInvalidArgumentException : ArithmeticException()
class ArcsinInvalidArgumentException : ArithmeticException()
class ArccosInvalidArgumentException : ArithmeticException()

internal object BigDecimalMath {
    private const val EXTRA_PRECISION_FOR_INTERNAL_CALCULATIONS: Int = 8
    private const val EXTRA_PRECISION_FOR_ROUND: Int = 2
    private const val EXTRA_PRECISION_FOR_CACHE: Int = 8
    private val NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
    private val ZERO: BigDecimal = BigDecimal.ZERO
    private val HALF: BigDecimal = "0.5".toBigDecimal()
    private val ONE: BigDecimal = BigDecimal.ONE
    private val TWO: BigDecimal = 2.toBigDecimal()
    private val FOUR: BigDecimal = 4.toBigDecimal()
    private val FIVE: BigDecimal = 5.toBigDecimal()
    private val TEN: BigDecimal = BigDecimal.TEN
    private val SIXTEEN: BigDecimal = 16.toBigDecimal()
    private val TWO_HUNDRED_THIRTY_NINE: BigDecimal = 239.toBigDecimal()
    private val BIG_INTEGER_ONE: BigInteger = BigInteger.ONE
    private val BIG_INTEGER_TWO: BigInteger = 2.toBigInteger()

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
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun pi(
        mc: MathContext,
    ): BigDecimal {
        return cachedPi(
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun factorial(
        value: Int,
    ): BigInteger {
        require(value >= 0)

        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var result: BigInteger = BigInteger.ONE
        var i = 1

        while (i <= value) {
            coroutineContext.ensureActive()

            result = result.multiply(i.toBigInteger())
            i++
        }

        return result
    }

    suspend fun sqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateSqrt(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun exp(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() == 0) {
            return ONE
        }

        return calculateExp(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun powNaturalExponent(
        base: BigDecimal,
        exponent: Int,
    ): BigDecimal = powNaturalExponent(
        base = base,
        exponent = exponent,
        one = ONE,
        multiply = BigDecimal::multiply
    )

    suspend fun powNaturalExponent(
        base: BigInteger,
        exponent: Int,
    ): BigInteger = powNaturalExponent(
        base = base,
        exponent = exponent,
        one = BIG_INTEGER_ONE,
        multiply = BigInteger::multiply
    )

    private suspend fun <T> powNaturalExponent(
        base: T,
        exponent: Int,
        one: T,
        multiply: (T, T) -> T,
    ): T {
        require(exponent >= 0)

        if (exponent == 0) {
            return one
        }

        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var n: Long = exponent.toLong()
        var currentBase: T = base
        var result: T = one

        while (n > 0L) {
            coroutineContext.ensureActive()

            if ((n and 1L) != 0L) {
                result = multiply(result, currentBase)
            }

            n = n shr 1

            if (n > 0L) {
                currentBase = multiply(currentBase, currentBase)
            }
        }
        return result
    }

    suspend fun powDecimalExponent(
        base: BigDecimal,
        exponent: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        require(base.signum() > 0)

        if (exponent.signum() == 0) {
            return ONE
        }

        val internalMc: MathContext = calculationMc(mc)

        return calculateExp(
            value = exponent.multiply(
                calculateLn(
                    value = base,
                    mc = internalMc,
                )
            ),
            mc = internalMc,
        ).round(mc)
    }

    suspend fun ln(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateLn(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun log(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val internalMc: MathContext = calculationMc(mc)

        return calculateLn(value, internalMc)
            .divide(cachedLnTen(internalMc), mc)
    }

    suspend fun sin(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateSin(
            radians = radians,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun cos(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateCos(
            radians = radians,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun arcsin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateArcsin(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun arccos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateArccos(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
    }

    suspend fun arctan(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return calculateArctan(
            value = value,
            mc = calculationMc(mc),
        ).round(mc)
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

            term = term.divide(n.toBigDecimal(), mc)
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

    private suspend fun calculateExp(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var reduced: BigDecimal = value
        var reductions = 0

        while (reduced.abs() > HALF) {
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
                .divide(n.toBigDecimal(), extendedMc)

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

        val lnX: BigDecimal = lnTaylor(
            value = x,
            mc = mc,
        )

        return lnX.add(
            exponent.toBigDecimal()
                .multiply(lnTwo)
        )
    }

    private suspend fun calculateSin(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val pi: BigDecimal = cachedPi(mc)
        val halfPi: BigDecimal = cachedHalfPi(mc)
        val threeHalfPi: BigDecimal = cachedThreeHalfPi(mc)
        val twoPi: BigDecimal = cachedTwoPi(mc)

        val normalizedRadians: BigDecimal =
            radians.toNormalizedRadians(mc)

        return when {
            normalizedRadians <= halfPi ->
                sinTaylor(
                    radians = normalizedRadians,
                    mc = mc,
                )

            normalizedRadians <= pi ->
                sinTaylor(
                    radians = pi.subtract(normalizedRadians),
                    mc = mc,
                )

            normalizedRadians <= threeHalfPi ->
                sinTaylor(
                    radians = normalizedRadians.subtract(pi),
                    mc = mc,
                ).negate()

            else ->
                sinTaylor(
                    radians = twoPi.subtract(normalizedRadians),
                    mc = mc,
                ).negate()
        }
    }

    private suspend fun calculateCos(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val pi: BigDecimal = cachedPi(mc)
        val halfPi: BigDecimal = cachedHalfPi(mc)
        val threeHalfPi: BigDecimal = cachedThreeHalfPi(mc)
        val twoPi: BigDecimal = cachedTwoPi(mc)

        val normalizedRadians: BigDecimal =
            radians.toNormalizedRadians(mc)


        return when {
            normalizedRadians <= halfPi ->
                cosTaylor(
                    radians = normalizedRadians,
                    mc = mc,
                )

            normalizedRadians <= pi ->
                cosTaylor(
                    radians = pi.subtract(normalizedRadians),
                    mc = mc,
                ).negate()

            normalizedRadians <= threeHalfPi ->
                cosTaylor(
                    radians = normalizedRadians.subtract(pi),
                    mc = mc,
                ).negate()

            else ->
                cosTaylor(
                    radians = twoPi.subtract(normalizedRadians),
                    mc = mc,
                )
        }
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

        val acuteAngle: BigDecimal = calculateInverseTrigonometricAcuteAngle(
            value = value,
            mc = mc,
        )

        val result: BigDecimal = cachedHalfPi(mc)
            .subtract(acuteAngle)

        return if (value.signum() < 0) {
            result.negate()
        } else {
            result
        }
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

        val acuteAngle: BigDecimal = calculateInverseTrigonometricAcuteAngle(
            value = value,
            mc = mc,
        )

        return if (value.signum() > 0) {
            acuteAngle
        } else {
            cachedPi(mc).subtract(acuteAngle)
        }
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

        if (value > HALF) {
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

    private suspend fun lnTaylor(
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

    private suspend fun sinTaylor(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val radiansSquared: BigDecimal = radians.multiply(radians)

        var term: BigDecimal = radians
        var sum: BigDecimal = radians
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            val twoN: BigInteger = BIG_INTEGER_TWO
                .multiply(n.toBigInteger())
            val denominator: BigInteger = twoN
                .multiply(twoN.add(BIG_INTEGER_ONE))

            term = term
                .multiply(radiansSquared)
                .divide(denominator.toBigDecimal(), mc)
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
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val radiansSquared: BigDecimal = radians.multiply(radians)

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
                .multiply(radiansSquared)
                .divide(denominator.toBigDecimal(), mc)
                .negate()

            val nextSum: BigDecimal = sum.add(term).round(mc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private suspend fun BigDecimal.toNormalizedRadians(
        mc: MathContext,
    ): BigDecimal {
        val twoPi: BigDecimal = cachedTwoPi(mc)

        return remainder(twoPi)
            .let { normalizedRadians ->
                if (normalizedRadians.signum() < 0) {
                    normalizedRadians.add(twoPi)
                } else {
                    normalizedRadians
                }
            }
    }

    private suspend fun calculateInverseTrigonometricAcuteAngle(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val absValue: BigDecimal = value.abs()

        val numerator: BigDecimal = ONE.subtract(absValue)
        val denominator: BigDecimal = ONE.add(absValue)

        val ratio: BigDecimal = numerator.divide(denominator, mc)
        val root: BigDecimal = calculateSqrt(
            value = ratio,
            mc = mc,
        )

        return TWO.multiply(
            calculateArctan(
                value = root,
                mc = mc,
            )
        )
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

    private suspend fun cachedLnTen(
        mc: MathContext,
    ): BigDecimal = cachedConstant(
        cachedValue = cachedLnTen,
        calculate = { mathContext ->
            calculateLn(
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
            lnTaylor(
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
            if ((cached.mc.precision >= (mc.precision + EXTRA_PRECISION_FOR_ROUND) &&
                        (cached.mc.roundingMode == mc.roundingMode))
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

    private fun calculationMc(
        mc: MathContext,
    ): MathContext = MathContext(
        mc.precision + EXTRA_PRECISION_FOR_INTERNAL_CALCULATIONS,
        mc.roundingMode,
    )
}

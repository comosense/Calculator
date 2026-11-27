package com.gmail.comosense.calculator.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.EnumMap
import kotlin.coroutines.CoroutineContext
import kotlin.enums.enumEntries

internal class SqrtInvalidArgumentException : ArithmeticException()
internal class LogInvalidArgumentException : ArithmeticException()
internal class ArcsinInvalidArgumentException : ArithmeticException()
internal class ArccosInvalidArgumentException : ArithmeticException()

internal object BigDecimalMath {
    private const val EXTRA_PRECISION_FOR_CALCULATIONS: Int = 8
    private const val EXTRA_PRECISION_FOR_ROUNDING: Int = 2
    private const val EXTRA_PRECISION_FOR_CACHE: Int = 8
    private val NEGATIVE_ONE: BigDecimal = BigDecimal.ONE.negate()
    private val ZERO: BigDecimal = BigDecimal.ZERO
    private val HALF: BigDecimal = "0.5".toBigDecimal()
    private val ONE: BigDecimal = BigDecimal.ONE
    private val THREE_HALVES: BigDecimal = "1.5".toBigDecimal()
    private val TWO: BigDecimal = 2.toBigDecimal()
    private val FOUR: BigDecimal = 4.toBigDecimal()
    private val FIVE: BigDecimal = 5.toBigDecimal()
    private val TEN: BigDecimal = BigDecimal.TEN
    private val SIXTEEN: BigDecimal = 16.toBigDecimal()
    private val TWO_HUNDRED_THIRTY_NINE: BigDecimal = 239.toBigDecimal()
    private val BIG_INTEGER_ZERO: BigInteger = BigInteger.ZERO
    private val BIG_INTEGER_ONE: BigInteger = BigInteger.ONE
    private val BIG_INTEGER_TWO: BigInteger = 2.toBigInteger()

    private enum class Quadrant {
        FIRST,
        SECOND,
        THIRD,
        FOURTH,
    }

    private enum class EConstant {
        E,
    }

    private enum class PiConstant {
        HALF,
        ONE,
        THREE_HALVES,
        TWO,
    }

    private enum class LnConstant {
        SQRT_TWO,
        INVERSE_SQRT_TWO,
        LN_TWO,
        LN_TEN,
    }

    private inline fun <reified E : Enum<E>> filledMap(
        transform: (E) -> BigDecimal,
    ): EnumMap<E, BigDecimal> =
        enumEntries<E>().associateWithTo(
            EnumMap(E::class.java),
            transform,
        )

    private abstract class ConstantCache<E : Enum<E>> {
        private data class Cache<E : Enum<E>>(
            val mc: MathContext,
            val map: EnumMap<E, BigDecimal>,
        )

        @Volatile
        private var cache: Cache<E>? = null

        protected abstract suspend fun calculateConstants(
            mc: MathContext,
        ): EnumMap<E, BigDecimal>

        suspend fun get(
            constant: E,
            mc: MathContext,
        ): BigDecimal =
            map(mc).getValue(constant).round(mc)

        private suspend fun map(
            mc: MathContext,
        ): Map<E, BigDecimal> {
            cache
                ?.takeIf { cached ->
                    cached.mc.precision >=
                            mc.precision + EXTRA_PRECISION_FOR_ROUNDING &&
                            cached.mc.roundingMode == mc.roundingMode
                }
                ?.let { cached -> return cached.map }

            val newMc: MathContext = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CACHE)
            val map: EnumMap<E, BigDecimal> = calculateConstants(newMc)

            cache = Cache(
                mc = newMc,
                map = map,
            )

            return map
        }
    }

    private class ECache : ConstantCache<EConstant>() {
        override suspend fun calculateConstants(
            mc: MathContext,
        ): EnumMap<EConstant, BigDecimal> {
            val e: BigDecimal = calculateE(
                mc = mc,
            )

            return filledMap { constant ->
                when (constant) {
                    EConstant.E -> e
                }
            }
        }
    }

    private class PiCache : ConstantCache<PiConstant>() {
        override suspend fun calculateConstants(
            mc: MathContext,
        ): EnumMap<PiConstant, BigDecimal> {
            val pi: BigDecimal = calculatePi(
                mc = mc,
            )

            return filledMap { constant ->
                when (constant) {
                    PiConstant.HALF -> pi.divide(TWO)
                    PiConstant.ONE -> pi
                    PiConstant.THREE_HALVES -> pi.multiply(THREE_HALVES)
                    PiConstant.TWO -> pi.multiply(TWO)
                }
            }
        }
    }

    private class LnCache : ConstantCache<LnConstant>() {
        override suspend fun calculateConstants(
            mc: MathContext,
        ): EnumMap<LnConstant, BigDecimal> {
            val sqrtTwo: BigDecimal = calculateSqrt(
                value = TWO,
                mc = mc,
            )
            val inverseSqrtTwo: BigDecimal = sqrtTwo.divide(TWO)
            val lnTwo: BigDecimal = lnTaylor(
                value = TWO,
                mc = mc,
            )
            val lnTen: BigDecimal = calculateLnMain(
                value = TEN,
                mc = mc,
                sqrtTwo = sqrtTwo,
                inverseSqrtTwo = inverseSqrtTwo,
                lnTwo = lnTwo,
            )

            return filledMap { constant ->
                when (constant) {
                    LnConstant.SQRT_TWO -> sqrtTwo
                    LnConstant.INVERSE_SQRT_TWO -> inverseSqrtTwo
                    LnConstant.LN_TWO -> lnTwo
                    LnConstant.LN_TEN -> lnTen
                }
            }
        }
    }

    private val eCache: ECache = ECache()
    private val piCache: PiCache = PiCache()
    private val lnCache: LnCache = LnCache()

    suspend fun e(
        mc: MathContext,
    ): BigDecimal =
        eCache.get(
            constant = EConstant.E,
            mc = mc,
        )

    suspend fun pi(
        mc: MathContext,
    ): BigDecimal =
        piCache.get(
            constant = PiConstant.ONE,
            mc = mc,
        )

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
    ): BigDecimal =
        calculateSqrt(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun exp(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() == 0) {
            return ONE
        }

        return calculateExp(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)
    }

    suspend fun powNaturalExponent(
        base: BigDecimal,
        exponent: Int,
        mc: MathContext,
    ): BigDecimal =
        powNaturalExponent(
            base = base,
            exponent = exponent,
            zero = ZERO,
            one = ONE,
            isZero = { value -> value.signum() == 0 },
            multiply = { value, other ->
                value.multiply(
                    other,
                    mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
                )
            },
        ).round(mc)

    suspend fun powNaturalExponent(
        base: BigInteger,
        exponent: Int,
    ): BigInteger =
        powNaturalExponent(
            base = base,
            exponent = exponent,
            zero = BIG_INTEGER_ZERO,
            one = BIG_INTEGER_ONE,
            isZero = { value -> value.signum() == 0 },
            multiply = BigInteger::multiply,
        )

    private suspend fun <T> powNaturalExponent(
        base: T,
        exponent: Int,
        zero: T,
        one: T,
        isZero: (T) -> Boolean,
        multiply: (T, T) -> T,
    ): T {
        require(exponent >= 0)

        if (exponent == 0) {
            return one
        }

        if (isZero(base)) {
            return zero
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
        require(base.signum() >= 0)

        if (exponent.signum() == 0) {
            return ONE
        }

        if (base.signum() == 0) {
            return ZERO
        }

        val calculationMc: MathContext = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS)
        val exponentProduct: BigDecimal = exponent.multiply(
            calculateLn(
                value = base,
                mc = calculationMc,
            ),
            calculationMc,
        )

        return calculateExp(
            value = exponentProduct,
            mc = calculationMc,
        ).round(mc)
    }

    suspend fun ln(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateLn(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun log(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val calculationMc: MathContext = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS)

        val lnValue: BigDecimal = calculateLn(
            value = value,
            mc = calculationMc,
        )
        val logValue: BigDecimal = lnValue.divide(
            lnCache.get(
                constant = LnConstant.LN_TEN,
                mc = calculationMc
            ),
            mc,
        )

        return logValue
    }

    suspend fun sin(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateSin(
            radians = radians,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun cos(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateCos(
            radians = radians,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun arcsin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateArcsin(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun arccos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateArccos(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

    suspend fun arctan(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateArctan(
            value = value,
            mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS),
        ).round(mc)

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
            val next: BigDecimal = sum.add(term, mc)

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
            value = ONE.divide(FIVE),
            mc = mc,
        )
        val arctanOneTwoHundredThirtyNine: BigDecimal = arctanTaylor(
            value = ONE.divide(TWO_HUNDRED_THIRTY_NINE, mc),
            mc = mc,
        )

        return SIXTEEN
            .multiply(arctanOneFifth, mc)
            .subtract(FOUR.multiply(arctanOneTwoHundredThirtyNine, mc), mc)
    }

    private suspend fun calculateSqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() < 0) {
            throw SqrtInvalidArgumentException()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val orderOfMagnitude: Int = value.precision() - value.scale() - 1
        val sqrtExponent: Int = Math.floorDiv(orderOfMagnitude, 2)

        var x: BigDecimal = ONE.scaleByPowerOfTen(sqrtExponent)
        var previousX: BigDecimal? = null

        while (true) {
            coroutineContext.ensureActive()

            val next: BigDecimal = x
                .add(value.divide(x, mc), mc)
                .divide(TWO)
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

        val extendedMc: MathContext =
            mc.withExtraPrecision(reductions)

        var sum: BigDecimal = ONE
        var term: BigDecimal = ONE
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            term = term
                .multiply(reduced, extendedMc)
                .divide(n.toBigDecimal(), extendedMc)

            val nextSum: BigDecimal = sum.add(term, extendedMc)

            if (nextSum.compareTo(sum) == 0) {
                sum = nextSum
                break
            }

            sum = nextSum
            n++
        }

        repeat(reductions) {
            coroutineContext.ensureActive()

            sum = sum.multiply(sum, extendedMc)
        }

        return sum.round(mc)
    }

    private suspend fun calculateLn(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() <= 0) {
            throw LogInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        return calculateLnMain(
            value = value,
            mc = mc,
            sqrtTwo = lnCache.get(
                constant = LnConstant.SQRT_TWO,
                mc = mc,
            ),
            inverseSqrtTwo = lnCache.get(
                constant = LnConstant.INVERSE_SQRT_TWO,
                mc = mc,
            ),
            lnTwo = lnCache.get(
                constant = LnConstant.LN_TWO,
                mc = mc,
            ),
        )
    }

    private suspend fun calculateLnMain(
        value: BigDecimal,
        mc: MathContext,
        sqrtTwo: BigDecimal,
        inverseSqrtTwo: BigDecimal,
        lnTwo: BigDecimal,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var x: BigDecimal = value
        var exponent = 0

        while (x > sqrtTwo) {
            coroutineContext.ensureActive()

            x = x.divide(TWO)
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
                .multiply(lnTwo, mc),
            mc,
        )
    }

    private suspend fun calculateSin(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateTrigonometric(
            radians = radians,
            mc = mc,
            taylor = ::sinTaylor,
            isNegative = { quadrant ->
                (quadrant == Quadrant.THIRD || quadrant == Quadrant.FOURTH) xor
                        (radians.signum() < 0)
            },
        )

    private suspend fun calculateCos(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal =
        calculateTrigonometric(
            radians = radians,
            mc = mc,
            taylor = ::cosTaylor,
            isNegative = { quadrant ->
                quadrant == Quadrant.SECOND || quadrant == Quadrant.THIRD
            },
        )

    private suspend fun calculateTrigonometric(
        radians: BigDecimal,
        mc: MathContext,
        taylor: suspend (BigDecimal, MathContext) -> BigDecimal,
        isNegative: (Quadrant) -> Boolean,
    ): BigDecimal {
        val pi: BigDecimal = piCache.get(
            constant = PiConstant.ONE,
            mc = mc,
        )
        val halfPi: BigDecimal = piCache.get(
            constant = PiConstant.HALF,
            mc = mc,
        )
        val threeHalvesPi: BigDecimal = piCache.get(
            constant = PiConstant.THREE_HALVES,
            mc = mc,
        )
        val twoPi: BigDecimal = piCache.get(
            constant = PiConstant.TWO,
            mc = mc,
        )

        val normalized: BigDecimal = radians.abs().remainder(twoPi)
        val (reduced: BigDecimal, quadrant: Quadrant) = when {
            normalized <= halfPi ->
                normalized to Quadrant.FIRST

            normalized <= pi ->
                pi.subtract(normalized, mc) to Quadrant.SECOND

            normalized <= threeHalvesPi ->
                normalized.subtract(pi, mc) to Quadrant.THIRD

            else ->
                twoPi.subtract(normalized, mc) to Quadrant.FOURTH
        }

        val result: BigDecimal = taylor(reduced, mc)

        return if (isNegative(quadrant)) {
            result.negate()
        } else {
            result
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
            return piCache.get(
                constant = PiConstant.HALF,
                mc = mc,
            )
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return piCache.get(
                constant = PiConstant.HALF,
                mc = mc,
            ).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val absValue: BigDecimal = value.abs()
        val result: BigDecimal =
            if (absValue <= HALF) {
                val valueSquared: BigDecimal =
                    absValue.multiply(absValue, mc)
                val denominator: BigDecimal =
                    calculateSqrt(
                        value = ONE.subtract(valueSquared, mc),
                        mc = mc,
                    )

                calculateArctan(
                    value = absValue.divide(denominator, mc),
                    mc = mc,
                )
            } else {
                val acuteAngle: BigDecimal =
                    calculateInverseTrigonometricAcuteAngle(
                        value = value,
                        mc = mc,
                    )

                piCache.get(
                    constant = PiConstant.HALF,
                    mc = mc,
                ).subtract(acuteAngle, mc)
            }

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
            return piCache.get(
                constant = PiConstant.ONE,
                mc = mc,
            )
        }

        if (value.signum() == 0) {
            return piCache.get(
                constant = PiConstant.HALF,
                mc = mc,
            )
        }

        val acuteAngle: BigDecimal = calculateInverseTrigonometricAcuteAngle(
            value = value,
            mc = mc,
        )

        return if (value.signum() > 0) {
            acuteAngle
        } else {
            piCache.get(
                constant = PiConstant.ONE,
                mc = mc,
            ).subtract(acuteAngle, mc)
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
            return piCache.get(
                constant = PiConstant.HALF,
                mc = mc,
            ).subtract(
                calculateArctan(
                    value = ONE.divide(value, mc),
                    mc = mc,
                ),
                mc,
            )
        }

        if (value > HALF) {
            val reduced: BigDecimal = value.subtract(ONE)
                .divide(value.add(ONE), mc)

            return piCache.get(
                constant = PiConstant.ONE,
                mc = mc,
            ).divide(FOUR)
                .add(
                    calculateArctan(
                        value = reduced,
                        mc = mc,
                    ),
                    mc,
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
        val zSquared: BigDecimal = z.multiply(z, mc)

        var term: BigDecimal = z
        var sum: BigDecimal = z
        var denominator: BigDecimal = ONE

        while (true) {
            coroutineContext.ensureActive()

            term = term.multiply(zSquared, mc)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(denominator, mc)
            val nextSum: BigDecimal = sum.add(nextTerm, mc)

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
        val radiansSquared: BigDecimal = radians.multiply(radians, mc)

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
                .multiply(radiansSquared, mc)
                .divide(denominator.toBigDecimal(), mc)
                .negate()

            val nextSum: BigDecimal = sum.add(term, mc)

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
        val radiansSquared: BigDecimal = radians.multiply(radians, mc)

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
                .multiply(radiansSquared, mc)
                .divide(denominator.toBigDecimal(), mc)
                .negate()

            val nextSum: BigDecimal = sum.add(term, mc)

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
        val valueSquared: BigDecimal = value.multiply(value, mc)

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var denominator: BigDecimal = ONE
        var sign: Int = -1

        while (true) {
            coroutineContext.ensureActive()

            term = term.multiply(valueSquared, mc)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(denominator, mc)

            val nextSum: BigDecimal = if (sign > 0) {
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

    private suspend fun calculateInverseTrigonometricAcuteAngle(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val absValue: BigDecimal = value.abs()
        val numerator: BigDecimal = ONE.subtract(absValue, mc)
        val denominator: BigDecimal = ONE.add(absValue, mc)
        val ratio: BigDecimal = numerator.divide(denominator, mc)
        val root: BigDecimal = calculateSqrt(
            value = ratio,
            mc = mc,
        )

        return TWO.multiply(
            calculateArctan(
                value = root,
                mc = mc,
            ),
            mc,
        )
    }
}

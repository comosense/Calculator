package com.gmail.comosense.calculator.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.coroutines.CoroutineContext

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
    private val ONE_HALF: BigDecimal = "1.5".toBigDecimal()
    private val TWO: BigDecimal = 2.toBigDecimal()
    private val FOUR: BigDecimal = 4.toBigDecimal()
    private val FIVE: BigDecimal = 5.toBigDecimal()
    private val TEN: BigDecimal = BigDecimal.TEN
    private val SIXTEEN: BigDecimal = 16.toBigDecimal()
    private val TWO_HUNDRED_THIRTY_NINE: BigDecimal = 239.toBigDecimal()
    private val BIG_INTEGER_ZERO: BigInteger = BigInteger.ZERO
    private val BIG_INTEGER_ONE: BigInteger = BigInteger.ONE
    private val BIG_INTEGER_TWO: BigInteger = 2.toBigInteger()

    private data class MathConstant(
        val mc: MathContext,
        val value: BigDecimal,
    )

    private class CachedConstant private constructor(
        private val dependencies: List<CachedConstant>,
        private val calculate: suspend (MathContext, List<BigDecimal>) -> BigDecimal,
    ) {
        private var extraPrecision: Int = EXTRA_PRECISION_FOR_CACHE

        @Volatile
        private var cached: MathConstant? = null

        init {
            propagateExtraPrecision()
        }

        private fun ensureExtraPrecision(
            required: Int,
        ) {
            if (required > extraPrecision) {
                extraPrecision = required
                propagateExtraPrecision()
            }
        }

        private fun propagateExtraPrecision() {
            val requiredForDependency: Int =
                extraPrecision + EXTRA_PRECISION_FOR_ROUNDING

            for (dependency in dependencies) {
                dependency.ensureExtraPrecision(requiredForDependency)
            }
        }

        suspend fun get(
            mc: MathContext,
        ): BigDecimal {
            cached?.let { current ->
                if (current.mc.hasBiggerPrecisionThan(
                        other = mc,
                        extraPrecision = EXTRA_PRECISION_FOR_ROUNDING
                    )
                ) {
                    return current.value.round(mc)
                }
            }

            val workingMc = mc.withExtraPrecision(extraPrecision)
            val dependencyValues: List<BigDecimal> =
                if (dependencies.isEmpty()) {
                    emptyList()
                } else {
                    dependencies.map { dependency ->
                        dependency.get(workingMc)
                    }
                }
            val value: BigDecimal =
                calculate(
                    workingMc,
                    dependencyValues,
                )

            cached = MathConstant(
                mc = workingMc,
                value = value,
            )

            return value.round(mc)
        }

        companion object {
            operator fun invoke(
                calculate: suspend (MathContext) -> BigDecimal,
            ): CachedConstant =
                CachedConstant(
                    dependencies = emptyList(),
                    calculate = { mc, _ -> calculate(mc) }
                )

            operator fun invoke(
                d1: CachedConstant,
                calculate: suspend (MathContext, BigDecimal) -> BigDecimal,
            ): CachedConstant =
                CachedConstant(
                    dependencies = listOf(d1),
                    calculate = { mc, deps -> calculate(mc, deps[0]) }
                )

            operator fun invoke(
                d1: CachedConstant,
                d2: CachedConstant,
                calculate: suspend (MathContext, BigDecimal, BigDecimal) -> BigDecimal,
            ): CachedConstant =
                CachedConstant(
                    dependencies = listOf(d1, d2),
                    calculate = { mc, deps -> calculate(mc, deps[0], deps[1]) }
                )

            operator fun invoke(
                d1: CachedConstant,
                d2: CachedConstant,
                d3: CachedConstant,
                calculate: suspend (MathContext, BigDecimal, BigDecimal, BigDecimal) -> BigDecimal,
            ): CachedConstant =
                CachedConstant(
                    dependencies = listOf(d1, d2, d3),
                    calculate = { mc, deps -> calculate(mc, deps[0], deps[1], deps[2]) }
                )
        }
    }

    private val cachedE = CachedConstant { workingMc ->
        calculateE(
            mc = workingMc,
        )
    }
    private val cachedPi = CachedConstant { workingMc ->
        calculatePi(
            mc = workingMc,
        )
    }
    private val cachedSqrtTwo = CachedConstant { workingMc ->
        calculateSqrt(
            value = TWO,
            mc = workingMc,
        )
    }
    private val cachedInverseSqrtTwo = CachedConstant(
        cachedSqrtTwo,
    ) { _, sqrtTwo ->
        sqrtTwo.divide(TWO)
    }
    private val cachedLnTwo = CachedConstant { workingMc ->
        lnTaylor(
            value = TWO,
            mc = workingMc,
        )
    }
    private val cachedLnTen = CachedConstant(
        cachedSqrtTwo,
        cachedInverseSqrtTwo,
        cachedLnTwo,
    ) { workingMc, sqrtTwo, inverseSqrtTwo, lnTwo ->
        calculateLnMain(
            value = TEN,
            mc = workingMc,
            sqrtTwo = sqrtTwo,
            inverseSqrtTwo = inverseSqrtTwo,
            lnTwo = lnTwo,
        )
    }
    private val cachedHalfPi = CachedConstant(
        cachedPi,
    ) { _, pi ->
        pi.divide(TWO)
    }
    private val cachedThreeHalfPi = CachedConstant(
        cachedPi,
    ) { workingMc, pi ->
        pi.multiply(ONE_HALF, workingMc)
    }
    private val cachedTwoPi = CachedConstant(
        cachedPi,
    ) { _, pi ->
        pi.multiply(TWO)
    }

    suspend fun e(
        mc: MathContext,
    ): BigDecimal =
        cachedE.get(mc)

    suspend fun pi(
        mc: MathContext,
    ): BigDecimal =
        cachedPi.get(mc)

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

        val calculationMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS)

        return calculateExp(
            value = exponent.multiply(
                calculateLn(
                    value = base,
                    mc = calculationMc,
                ),
                calculationMc,
            ),
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
        val calculationMc: MathContext =
            mc.withExtraPrecision(EXTRA_PRECISION_FOR_CALCULATIONS)

        return calculateLn(
            value = value,
            mc = calculationMc,
        )
            .divide(lnTen(calculationMc), mc)
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
            .multiply(
                arctanOneFifth,
                mc,
            )
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine, mc,
                ),
                mc,
            )
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
            sqrtTwo = sqrtTwo(mc),
            inverseSqrtTwo = inverseSqrtTwo(mc),
            lnTwo = lnTwo(mc),
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
    ): BigDecimal {
        val pi: BigDecimal = pi(mc)
        val halfPi: BigDecimal = halfPi(mc)
        val threeHalfPi: BigDecimal = threeHalfPi(mc)
        val twoPi: BigDecimal = twoPi(mc)

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
                    radians = pi.subtract(
                        normalizedRadians,
                        mc,
                    ),
                    mc = mc,
                )

            normalizedRadians <= threeHalfPi ->
                sinTaylor(
                    radians = normalizedRadians.subtract(
                        pi,
                        mc,
                    ),
                    mc = mc,
                ).negate()

            else ->
                sinTaylor(
                    radians = twoPi.subtract(
                        normalizedRadians,
                        mc,
                    ),
                    mc = mc,
                ).negate()
        }
    }

    private suspend fun calculateCos(
        radians: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val pi: BigDecimal = pi(mc)
        val halfPi: BigDecimal = halfPi(mc)
        val threeHalfPi: BigDecimal = threeHalfPi(mc)
        val twoPi: BigDecimal = twoPi(mc)

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
                    radians = pi.subtract(
                        normalizedRadians,
                        mc,
                    ),
                    mc = mc,
                ).negate()

            normalizedRadians <= threeHalfPi ->
                cosTaylor(
                    radians = normalizedRadians.subtract(
                        pi,
                        mc,
                    ),
                    mc = mc,
                ).negate()

            else ->
                cosTaylor(
                    radians = twoPi.subtract(
                        normalizedRadians,
                        mc,
                    ),
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
            return pi(mc).divide(TWO)
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return pi(mc).divide(TWO).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val acuteAngle: BigDecimal = calculateInverseTrigonometricAcuteAngle(
            value = value,
            mc = mc,
        )

        val result: BigDecimal = halfPi(mc)
            .subtract(acuteAngle, mc)

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
            return pi(mc)
        }

        if (value.signum() == 0) {
            return pi(mc).divide(TWO)
        }

        val acuteAngle: BigDecimal = calculateInverseTrigonometricAcuteAngle(
            value = value,
            mc = mc,
        )

        return if (value.signum() > 0) {
            acuteAngle
        } else {
            pi(mc).subtract(acuteAngle, mc)
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
            return pi(mc).divide(TWO)
                .subtract(
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

            return pi(mc).divide(FOUR)
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

    private suspend fun BigDecimal.toNormalizedRadians(
        mc: MathContext,
    ): BigDecimal {
        val twoPi: BigDecimal = twoPi(mc)

        return remainder(twoPi)
            .let { normalizedRadians ->
                if (normalizedRadians.signum() < 0) {
                    normalizedRadians.add(twoPi, mc)
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

    private suspend fun sqrtTwo(
        mc: MathContext,
    ): BigDecimal =
        cachedSqrtTwo.get(mc)

    private suspend fun inverseSqrtTwo(
        mc: MathContext,
    ): BigDecimal =
        cachedInverseSqrtTwo.get(mc)

    private suspend fun lnTwo(
        mc: MathContext,
    ): BigDecimal =
        cachedLnTwo.get(mc)

    private suspend fun lnTen(
        mc: MathContext,
    ): BigDecimal =
        cachedLnTen.get(mc)

    private suspend fun halfPi(
        mc: MathContext,
    ): BigDecimal =
        cachedHalfPi.get(mc)

    private suspend fun threeHalfPi(
        mc: MathContext,
    ): BigDecimal =
        cachedThreeHalfPi.get(mc)

    private suspend fun twoPi(
        mc: MathContext,
    ): BigDecimal =
        cachedTwoPi.get(mc)
}

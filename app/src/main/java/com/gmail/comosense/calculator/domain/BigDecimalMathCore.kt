package com.gmail.comosense.calculator.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.coroutines.CoroutineContext

class SqrtInvalidArgumentException : ArithmeticException()
class LogInvalidArgumentException : ArithmeticException()
class TanInvalidArgumentException : ArithmeticException()
class ArcsinInvalidArgumentException : ArithmeticException()
class ArccosInvalidArgumentException : ArithmeticException()

internal class BigDecimalMathCore(
    private val coreMc: MathContext,
    private val tolerancePrecision: Int,
) {
    init {
        require(coreMc.precision > 0)
        require(tolerancePrecision > 0)
    }

    companion object {
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
    }

    private data class ConstantsForLn(
        val sqrtTwo: BigDecimal,
        val inverseSqrtTwo: BigDecimal,
        val lnTwo: BigDecimal,
    )

    private data class PiConstants(
        val pi: BigDecimal,
        val halfPi: BigDecimal,
        val threeHalfPi: BigDecimal,
        val twoPi: BigDecimal,
    )

    private val tolerance: BigDecimal by lazy {
        ONE.scaleByPowerOfTen(-tolerancePrecision)
    }

    @Volatile
    private var cachedE: BigDecimal? = null

    @Volatile
    private var cachedPi: BigDecimal? = null

    @Volatile
    private var cachedLnTen: BigDecimal? = null
    private suspend fun cachedLnTen(): BigDecimal {
        cachedLnTen?.let { cached ->
            return cached
        }

        val lnTen = ln(TEN)
        cachedLnTen = lnTen

        return lnTen
    }

    @Volatile
    private var cachedConstantsForLn: ConstantsForLn? = null
    private suspend fun cachedConstantsForLn(): ConstantsForLn {
        cachedConstantsForLn?.let { cached ->
            return cached
        }

        val sqrtTwo: BigDecimal = sqrt(TWO)
        val constantsForLn = ConstantsForLn(
            sqrtTwo = sqrtTwo,
            inverseSqrtTwo = ONE.divide(sqrtTwo, coreMc),
            lnTwo = calculateLn(TWO),
        )
        cachedConstantsForLn = constantsForLn

        return constantsForLn
    }

    @Volatile
    private var cachedPiConstants: PiConstants? = null
    private suspend fun cachedPiConstants(): PiConstants {
        cachedPiConstants?.let { cached ->
            return cached
        }

        val pi: BigDecimal = pi()
        val halfPi: BigDecimal = pi.divide(TWO)
        val threeHalfPi: BigDecimal = pi.add(halfPi)
        val twoPi: BigDecimal = pi.multiply(TWO)

        val piConstants = PiConstants(
            pi = pi,
            halfPi = halfPi,
            threeHalfPi = threeHalfPi,
            twoPi = twoPi,
        )
        cachedPiConstants = piConstants

        return piConstants
    }

    suspend fun e(): BigDecimal {
        cachedE?.let { cached ->
            return cached
        }

        val e = calculateE()
        cachedE = e

        return e
    }

    suspend fun pi(): BigDecimal {
        cachedPi?.let { cached ->
            return cached
        }

        val pi = calculatePi()
        cachedPi = pi

        return pi
    }

    suspend fun factorialUnrounded(value: BigInteger): BigDecimal {
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

    suspend fun sqrt(value: BigDecimal): BigDecimal {
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
                .add(value.divide(x, coreMc))
                .divide(TWO, coreMc)
            if (next.compareTo(x) == 0 ||
                (previousX != null && next.compareTo(previousX) == 0)
            ) {
                return minOf(x, next)
            }
            previousX = x
            x = next
        }
    }

    fun powUnrounded(base: BigDecimal, exponent: Int): BigDecimal {
        return base.pow(exponent)
    }

    suspend fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        return exp(exponent.multiply(ln(base)))
    }

    suspend fun ln(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        if (value.signum() <= 0) {
            throw LogInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        val constantsForLn: ConstantsForLn = cachedConstantsForLn()
        var x: BigDecimal = value
        var exponent = 0

        while (x > constantsForLn.sqrtTwo) {
            coroutineContext.ensureActive()

            x = x.divide(TWO, coreMc)
            exponent++
        }

        while (x < constantsForLn.inverseSqrtTwo) {
            coroutineContext.ensureActive()

            x = x.multiply(TWO)
            exponent--
        }

        val lnX: BigDecimal = calculateLn(x)

        return lnX.add(
            BigDecimal(exponent)
                .multiply(constantsForLn.lnTwo)
        )
    }

    suspend fun log(value: BigDecimal): BigDecimal {
        return ln(value)
            .divide(cachedLnTen(), coreMc)
    }

    suspend fun sin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return calculateSin(
            if (isRadians) {
                value
            } else {
                value.toRadians()
            }
        )
    }

    suspend fun cos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return calculateCos(
            if (isRadians) {
                value
            } else {
                value.toRadians()
            }
        )
    }

    suspend fun tan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return calculateTan(
            if (isRadians) {
                value
            } else {
                value.toRadians()
            }
        )
    }

    suspend fun arcsin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = calculateArcsin(value)

        return if (isRadians) {
            result
        } else {
            result.toDegrees()
        }
    }

    suspend fun arccos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = calculateArccos(value)

        return if (isRadians) {
            result
        } else {
            result.toDegrees()
        }
    }

    suspend fun arctan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = calculateArctan(value)

        return if (isRadians) {
            result
        } else {
            result.toDegrees()
        }
    }

    private suspend fun calculateE(): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var sum: BigDecimal = ONE
        var term: BigDecimal = ONE
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            term = term.divide(BigDecimal(n), coreMc)
            val next: BigDecimal = sum.add(term).round(coreMc)

            if (next.compareTo(sum) == 0) {
                return next
            }

            sum = next
            n++
        }
    }

    private suspend fun calculatePi(): BigDecimal {
        val arctanOneFifth: BigDecimal =
            arctanTaylor(ONE.divide(FIVE, coreMc))
        val arctanOneTwoHundredThirtyNine: BigDecimal =
            arctanTaylor(ONE.divide(TWO_HUNDRED_THIRTY_NINE, coreMc))

        return SIXTEEN
            .multiply(arctanOneFifth)
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine
                )
            )
    }

    private suspend fun exp(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var reduced: BigDecimal = value
        var reductions = 0

        while (reduced.abs() > ONE_HALF) {
            coroutineContext.ensureActive()

            reduced = reduced.divide(TWO)
            reductions++
        }

        val extendedMc = MathContext(
            coreMc.precision + reductions,
            coreMc.roundingMode,
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

    private suspend fun calculateLn(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val z: BigDecimal = value
            .subtract(ONE)
            .divide(value.add(ONE), coreMc)
        val zSquared: BigDecimal = z.multiply(z)

        var term: BigDecimal = z
        var sum: BigDecimal = z
        var denominator: BigDecimal = ONE

        while (true) {
            coroutineContext.ensureActive()

            term = term.multiply(zSquared)
            denominator = denominator.add(TWO)

            val nextTerm: BigDecimal = term.divide(denominator, coreMc)
            val nextSum: BigDecimal = sum.add(nextTerm).round(coreMc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum.multiply(TWO)
            }

            sum = nextSum
        }
    }

    private suspend fun calculateSin(value: BigDecimal): BigDecimal {
        val angle: BigDecimal = value.normalizedAngle()
        val piConstants: PiConstants = cachedPiConstants()

        val distanceToZero: BigDecimal =
            minOf(angle, piConstants.twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(piConstants.halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(piConstants.pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(piConstants.threeHalfPi).abs()

        return when {
            distanceToZero <= tolerance || distanceToPi <= tolerance ->
                ZERO

            distanceToHalfPi <= tolerance ->
                ONE

            distanceToThreeHalfPi <= tolerance ->
                NEGATIVE_ONE

            angle <= piConstants.halfPi ->
                sinTaylor(angle)

            angle <= piConstants.pi ->
                sinTaylor(piConstants.pi.subtract(angle))

            angle <= piConstants.threeHalfPi ->
                sinTaylor(angle.subtract(piConstants.pi))
                    .negate()

            else ->
                sinTaylor(piConstants.twoPi.subtract(angle))
                    .negate()
        }
    }

    private suspend fun calculateCos(value: BigDecimal): BigDecimal {
        val angle: BigDecimal = value.normalizedAngle()
        val piConstants: PiConstants = cachedPiConstants()

        val distanceToZero: BigDecimal =
            minOf(angle, piConstants.twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(piConstants.halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(piConstants.pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(piConstants.threeHalfPi).abs()

        return when {
            distanceToHalfPi <= tolerance || distanceToThreeHalfPi <= tolerance ->
                ZERO

            distanceToZero <= tolerance ->
                ONE

            distanceToPi <= tolerance ->
                NEGATIVE_ONE

            angle <= piConstants.halfPi ->
                cosTaylor(angle)

            angle <= piConstants.pi ->
                cosTaylor(piConstants.pi.subtract(angle))
                    .negate()

            angle <= piConstants.threeHalfPi ->
                cosTaylor(angle.subtract(piConstants.pi))
                    .negate()

            else ->
                cosTaylor(piConstants.twoPi.subtract(angle))
        }
    }

    private suspend fun calculateTan(value: BigDecimal): BigDecimal {
        val cos: BigDecimal = calculateCos(value)

        if (cos.signum() == 0) {
            throw TanInvalidArgumentException()
        }

        val sin: BigDecimal = calculateSin(value)

        return sin.divide(cos, coreMc)
    }

    private suspend fun calculateArcsin(value: BigDecimal): BigDecimal {
        if (value !in NEGATIVE_ONE..ONE) {
            throw ArcsinInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return cachedPiConstants().pi.divide(TWO, coreMc)
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return cachedPiConstants().pi.divide(TWO, coreMc).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val absValue: BigDecimal = value.abs()
        val oneMinusXSquared: BigDecimal = ONE
            .subtract(absValue)
            .multiply(ONE.add(absValue))
        val root: BigDecimal = sqrt(oneMinusXSquared)

        return calculateArctan(value.divide(root, coreMc))
    }

    private suspend fun calculateArccos(value: BigDecimal): BigDecimal {
        if (value !in NEGATIVE_ONE..ONE) {
            throw ArccosInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return ZERO
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return cachedPiConstants().pi
        }

        if (value.signum() == 0) {
            return cachedPiConstants().pi.divide(TWO, coreMc)
        }

        if (value.signum() < 0) {
            return cachedPiConstants().pi.subtract(calculateArccos(value.negate()))
        }

        val numerator: BigDecimal = ONE.subtract(value)
        val denominator: BigDecimal = ONE.add(value)
        val ratio: BigDecimal = numerator.divide(denominator, coreMc)
        val root: BigDecimal = sqrt(ratio)

        return TWO.multiply(calculateArctan(root))
    }

    private suspend fun calculateArctan(value: BigDecimal): BigDecimal {
        if (value.signum() < 0) {
            return calculateArctan(value.negate()).negate()
        }

        if (value > ONE) {
            return cachedPiConstants().pi.divide(TWO, coreMc)
                .subtract(
                    calculateArctan(ONE.divide(value, coreMc))
                )
        }

        if (value > ONE_HALF) {
            val reduced: BigDecimal = value.subtract(ONE)
                .divide(value.add(ONE), coreMc)

            return cachedPiConstants().pi.divide(FOUR, coreMc)
                .add(calculateArctan(reduced))
        }

        return arctanTaylor(value)
    }

    private suspend fun sinTaylor(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val valueSquared: BigDecimal = value.multiply(value)

        var term: BigDecimal = value
        var sum: BigDecimal = value
        var n = 1

        while (true) {
            coroutineContext.ensureActive()

            val twoN: BigInteger = BIG_INTEGER_TWO
                .multiply(n.toBigInteger())
            val denominator: BigInteger = twoN
                .multiply(twoN.add(BIG_INTEGER_ONE))

            term = term
                .multiply(valueSquared)
                .divide(BigDecimal(denominator), coreMc)
                .negate()

            val nextSum: BigDecimal = sum.add(term).round(coreMc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private suspend fun cosTaylor(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val valueSquared: BigDecimal = value.multiply(value)

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
                .multiply(valueSquared)
                .divide(BigDecimal(denominator), coreMc)
                .negate()

            val nextSum: BigDecimal = sum.add(term).round(coreMc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            n++
        }
    }

    private suspend fun arctanTaylor(value: BigDecimal): BigDecimal {
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

            val nextTerm: BigDecimal = term.divide(denominator, coreMc)

            val nextSum: BigDecimal = if (sign > 0) {
                sum.add(nextTerm)
            } else {
                sum.subtract(nextTerm)
            }.round(coreMc)

            if (nextSum.compareTo(sum) == 0) {
                return nextSum
            }

            sum = nextSum
            sign = -sign
        }
    }

    private suspend fun BigDecimal.toRadians(): BigDecimal {
        return this
            .multiply(cachedPiConstants().pi)
            .divide(ONE_HUNDRED_EIGHTY, coreMc)
    }

    private suspend fun BigDecimal.toDegrees(): BigDecimal {
        return this
            .multiply(ONE_HUNDRED_EIGHTY)
            .divide(cachedPiConstants().pi, coreMc)
    }

    private suspend fun BigDecimal.normalizedAngle(): BigDecimal {
        val piConstants: PiConstants = cachedPiConstants()

        var angle: BigDecimal = this.remainder(piConstants.twoPi)

        if (angle.signum() < 0) {
            angle = angle.add(piConstants.twoPi)
        }

        return angle
    }
}

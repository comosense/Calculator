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
class SqrtInvalidArgumentException : ArithmeticException()
class PowInvalidArgumentException : ArithmeticException()
class PowLimitExceededException : ArithmeticException()
class LogInvalidArgumentException : ArithmeticException()
class TanInvalidArgumentException : ArithmeticException()
class ArcsinInvalidArgumentException : ArithmeticException()
class ArccosInvalidArgumentException : ArithmeticException()

class BigDecimalMath(private val mathContext: MathContext) {
    init {
        require(mathContext.precision > 0)
    }

    companion object {
        private const val EXTRA_PRECISION_FOR_CONSTANT: Int = 2
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 8
        private const val TOLERANCE_PRECISION_MARGIN: Int = EXTRA_PRECISION_FOR_WORKING + 2
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

    private data class ConstantsForLn(
        val mc: MathContext,
        val sqrtTwo: BigDecimal,
        val inverseSqrtTwo: BigDecimal,
        val lnTwo: BigDecimal,
    )

    private data class PiConstants(
        val mc: MathContext,
        val pi: BigDecimal,
        val halfPi: BigDecimal,
        val threeHalfPi: BigDecimal,
        val twoPi: BigDecimal,
    )

    private val mathContextForConstant: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION_FOR_CONSTANT,
        mathContext.roundingMode,
    )
    private val workingMc: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION_FOR_WORKING,
        mathContext.roundingMode,
    )

    @Volatile
    private var cachedTolerance: MathConstant? = null
    private fun cachedTolerance(mc: MathContext): MathConstant {
        cachedTolerance?.let { cached ->
            if (cached.mc == mc) {
                return cached
            }
        }

        val tolerance = MathConstant(
            mc = mc,
            value = ONE.scaleByPowerOfTen(-mc.precision + TOLERANCE_PRECISION_MARGIN),
        )
        cachedTolerance = tolerance

        return tolerance
    }

    @Volatile
    private var cachedE: MathConstant? = null
    private suspend fun cachedE(mc: MathContext): MathConstant {
        cachedE?.let { cached ->
            if (cached.mc == mc) {
                return cached
            }
        }

        val e = MathConstant(
            mc = mc,
            value = e(mc),
        )
        cachedE = e

        return e
    }

    @Volatile
    private var cachedLnTen: MathConstant? = null
    private suspend fun cachedLnTen(mc: MathContext): MathConstant {
        cachedLnTen?.let { cached ->
            if (cached.mc == mc) {
                return cached
            }
        }

        val lnTen = MathConstant(
            mc = mc,
            value = ln(TEN, mc),
        )
        cachedLnTen = lnTen

        return lnTen
    }

    @Volatile
    private var cachedConstantsForLn: ConstantsForLn? = null
    private suspend fun cachedConstantsForLn(mc: MathContext): ConstantsForLn {
        cachedConstantsForLn?.let { cached ->
            if (cached.mc == mc) {
                return cached
            }
        }

        val sqrtTwo: BigDecimal = sqrt(TWO, mc)
        val constantsForLn = ConstantsForLn(
            mc = mc,
            sqrtTwo = sqrtTwo,
            inverseSqrtTwo = ONE.divide(sqrtTwo, mc),
            lnTwo = calculateLn(TWO, mc),
        )
        cachedConstantsForLn = constantsForLn

        return constantsForLn
    }

    @Volatile
    private var cachedPiConstants: PiConstants? = null
    private suspend fun cachedPiConstants(mc: MathContext): PiConstants {
        cachedPiConstants?.let { cached ->
            if (cached.mc == mc) {
                return cached
            }
        }

        val pi: BigDecimal = pi(mc)
        val halfPi: BigDecimal = pi.divide(TWO, mc)
        val threeHalfPi: BigDecimal = pi.add(halfPi)
        val twoPi: BigDecimal = pi.multiply(TWO)

        val piConstants = PiConstants(
            mc = mc,
            pi = pi,
            halfPi = halfPi,
            threeHalfPi = threeHalfPi,
            twoPi = twoPi,
        )
        cachedPiConstants = piConstants

        return piConstants
    }

    suspend fun e(): BigDecimal {
        return cachedE(workingMc).value
            .round(mathContextForConstant)
    }

    suspend fun pi(): BigDecimal {
        return cachedPiConstants(workingMc).pi
            .round(mathContextForConstant)
    }

    suspend fun factorial(value: BigDecimal): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

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

        var result: BigInteger = BigInteger.ONE
        var i: BigInteger = BigInteger.ONE

        while (i <= integerValue) {
            coroutineContext.ensureActive()

            result = result.multiply(i)
            i = i.add(BigInteger.ONE)
        }

        return BigDecimal(result)
    }

    suspend fun sqrt(value: BigDecimal): BigDecimal {
        return sqrt(value, workingMc)
            .round(mathContext)
    }

    suspend fun pow(
        base: BigDecimal,
        exponent: BigDecimal,
    ): BigDecimal {
        if (base.signum() == 0) {
            return if (exponent.signum() > 0) {
                ZERO
            } else {
                throw PowInvalidArgumentException()
            }
        }

        val integerExponent: BigInteger? = try {
            exponent.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            null
        }

        return if (integerExponent != null) {
            powIntegerExponent(base, integerExponent, workingMc)
        } else {
            if (base.signum() < 0) {
                throw PowInvalidArgumentException()
            }
            powNonIntegerExponent(base, exponent, workingMc)
                .round(mathContext)
        }
    }

    suspend fun ln(value: BigDecimal): BigDecimal {
        return ln(value, workingMc)
            .round(mathContext)
    }

    suspend fun log(value: BigDecimal): BigDecimal {
        return ln(value, workingMc)
            .divide(cachedLnTen(workingMc).value, workingMc)
            .round(mathContext)
    }

    suspend fun sin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return sin(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workingMc)
            },
            workingMc
        ).round(mathContext)
    }

    suspend fun cos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return cos(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workingMc)
            },
            workingMc
        ).round(mathContext)
    }

    suspend fun tan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val cos: BigDecimal = cos(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workingMc)
            },
            workingMc
        )

        if (cos.signum() == 0) {
            throw TanInvalidArgumentException()
        }

        val sin: BigDecimal = sin(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workingMc)
            },
            workingMc
        )

        return sin.divide(cos, workingMc)
            .round(mathContext)
    }

    suspend fun arcsin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arcsin(value, workingMc)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workingMc)
        }.round(mathContext)
    }

    suspend fun arccos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arccos(value, workingMc)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workingMc)
        }.round(mathContext)
    }

    suspend fun arctan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arctan(value, workingMc)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workingMc)
        }.round(mathContext)
    }

    private suspend fun e(mc: MathContext): BigDecimal {
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

    private suspend fun pi(mc: MathContext): BigDecimal {
        val arctanOneFifth: BigDecimal =
            arctanTaylor(ONE.divide(FIVE, mc), mc)
        val arctanOneTwoHundredThirtyNine: BigDecimal =
            arctanTaylor(ONE.divide(TWO_HUNDRED_THIRTY_NINE, mc), mc)

        return SIXTEEN
            .multiply(arctanOneFifth)
            .subtract(
                FOUR.multiply(
                    arctanOneTwoHundredThirtyNine
                )
            )
    }

    private suspend fun sqrt(
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

    private fun powIntegerExponent(
        base: BigDecimal,
        exponent: BigInteger,
        mc: MathContext,
    ): BigDecimal {
        if (exponent.abs() > MAX_INTEGER_EXPONENT) {
            throw PowLimitExceededException()
        }

        val result: BigDecimal =
            base.pow(exponent.abs().intValueExact())

        return if (exponent.signum() < 0) {
            ONE.divide(result, mc)
        } else {
            result
        }
    }

    private suspend fun powNonIntegerExponent(
        base: BigDecimal,
        exponent: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (exponent.abs() > MAX_NON_INTEGER_EXPONENT) {
            throw PowLimitExceededException()
        }

        val value: BigDecimal =
            exponent.multiply(ln(base, mc))

        return exp(value, mc)
    }

    private suspend fun exp(
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

    private suspend fun ln(
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

        val constantsForLn: ConstantsForLn = cachedConstantsForLn(mc)
        var x: BigDecimal = value
        var exponent = 0

        while (x > constantsForLn.sqrtTwo) {
            coroutineContext.ensureActive()

            x = x.divide(TWO, mc)
            exponent++
        }

        while (x < constantsForLn.inverseSqrtTwo) {
            coroutineContext.ensureActive()

            x = x.multiply(TWO)
            exponent--
        }

        val lnX: BigDecimal = calculateLn(x, mc)

        return lnX.add(
            BigDecimal(exponent)
                .multiply(constantsForLn.lnTwo)
        )
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

    private suspend fun sin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val angle: BigDecimal = value.normalizedAngle(mc)
        val tolerance: MathConstant = cachedTolerance(mc)
        val piConstants: PiConstants = cachedPiConstants(mc)

        val distanceToZero: BigDecimal =
            minOf(angle, piConstants.twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(piConstants.halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(piConstants.pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(piConstants.threeHalfPi).abs()

        return when {
            distanceToZero <= tolerance.value || distanceToPi <= tolerance.value ->
                ZERO

            distanceToHalfPi <= tolerance.value ->
                ONE

            distanceToThreeHalfPi <= tolerance.value ->
                NEGATIVE_ONE

            angle <= piConstants.halfPi ->
                sinTaylor(angle, mc)

            angle <= piConstants.pi ->
                sinTaylor(piConstants.pi.subtract(angle), mc)

            angle <= piConstants.threeHalfPi ->
                sinTaylor(angle.subtract(piConstants.pi), mc)
                    .negate()

            else ->
                sinTaylor(piConstants.twoPi.subtract(angle), mc)
                    .negate()
        }
    }

    private suspend fun cos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val angle: BigDecimal = value.normalizedAngle(mc)
        val tolerance: MathConstant = cachedTolerance(mc)
        val piConstants: PiConstants = cachedPiConstants(mc)

        val distanceToZero: BigDecimal =
            minOf(angle, piConstants.twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(piConstants.halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(piConstants.pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(piConstants.threeHalfPi).abs()

        return when {
            distanceToHalfPi <= tolerance.value || distanceToThreeHalfPi <= tolerance.value ->
                ZERO

            distanceToZero <= tolerance.value ->
                ONE

            distanceToPi <= tolerance.value ->
                NEGATIVE_ONE

            angle <= piConstants.halfPi ->
                cosTaylor(angle, mc)

            angle <= piConstants.pi ->
                cosTaylor(piConstants.pi.subtract(angle), mc)
                    .negate()

            angle <= piConstants.threeHalfPi ->
                cosTaylor(angle.subtract(piConstants.pi), mc)
                    .negate()

            else ->
                cosTaylor(piConstants.twoPi.subtract(angle), mc)
        }
    }

    private suspend fun arcsin(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value !in NEGATIVE_ONE..ONE) {
            throw ArcsinInvalidArgumentException()
        }

        if (value.compareTo(ONE) == 0) {
            return cachedPiConstants(mc).pi.divide(TWO, mc)
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return cachedPiConstants(mc).pi.divide(TWO, mc).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val absValue: BigDecimal = value.abs()
        val oneMinusXSquared: BigDecimal = ONE
            .subtract(absValue)
            .multiply(ONE.add(absValue))
        val root: BigDecimal = sqrt(oneMinusXSquared, mc)

        return arctan(value.divide(root, mc), mc)
    }

    private suspend fun arccos(
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
            return cachedPiConstants(mc).pi
        }

        if (value.signum() == 0) {
            return cachedPiConstants(mc).pi.divide(TWO, mc)
        }

        if (value.signum() < 0) {
            return cachedPiConstants(mc).pi.subtract(arccos(value.negate(), mc))
        }

        val numerator: BigDecimal = ONE.subtract(value)
        val denominator: BigDecimal = ONE.add(value)
        val ratio: BigDecimal = numerator.divide(denominator, mc)
        val root: BigDecimal = sqrt(ratio, mc)

        return TWO.multiply(arctan(root, mc))
    }

    private suspend fun arctan(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        if (value.signum() < 0) {
            return arctan(value.negate(), mc).negate()
        }

        if (value > ONE) {
            return cachedPiConstants(mc).pi.divide(TWO, mc)
                .subtract(
                    arctan(ONE.divide(value, mc), mc)
                )
        }

        if (value > ONE_HALF) {
            val reduced: BigDecimal = value.subtract(ONE)
                .divide(value.add(ONE), mc)

            return cachedPiConstants(mc).pi.divide(FOUR, mc)
                .add(arctan(reduced, mc))
        }

        return arctanTaylor(value, mc)
    }

    private suspend fun sinTaylor(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
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
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
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
        mc: MathContext,
    ): BigDecimal {
        return this
            .multiply(cachedPiConstants(mc).pi)
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }

    private suspend fun BigDecimal.toDegrees(
        mc: MathContext,
    ): BigDecimal {
        return this
            .multiply(ONE_HUNDRED_EIGHTY)
            .divide(cachedPiConstants(mc).pi, mc)
    }

    private suspend fun BigDecimal.normalizedAngle(
        mc: MathContext,
    ): BigDecimal {
        val piConstants: PiConstants = cachedPiConstants(mc)

        var angle: BigDecimal = this.remainder(piConstants.twoPi, mc)

        if (angle.signum() < 0) {
            angle = angle.add(piConstants.twoPi)
        }

        return angle
    }
}

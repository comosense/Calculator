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
        private const val EXTRA_PRECISION: Int = 8
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

    private val workMathContext: MathContext = MathContext(
        mathContext.precision + EXTRA_PRECISION,
        mathContext.roundingMode,
    )

    @Volatile
    private var cachedSqrtTwo: BigDecimal? = null
    private suspend fun sqrtTwo(): BigDecimal {
        cachedSqrtTwo?.let { sqrtTwo -> return sqrtTwo }

        val result: BigDecimal = sqrt(TWO, workMathContext)
        cachedSqrtTwo = result

        return result
    }

    @Volatile
    private var cachedInverseSqrtTwo: BigDecimal? = null
    private suspend fun inverseSqrtTwo(): BigDecimal {
        cachedInverseSqrtTwo?.let { inverseSqrtTwo -> return inverseSqrtTwo }

        val result: BigDecimal = ONE.divide(sqrtTwo(), workMathContext)
        cachedInverseSqrtTwo = result

        return result
    }

    @Volatile
    private var cachedLnTwo: BigDecimal? = null
    private suspend fun lnTwo(): BigDecimal {
        cachedLnTwo?.let { lnTwo -> return lnTwo }

        val result: BigDecimal = calculateLn(TWO, workMathContext)
        cachedLnTwo = result

        return result
    }

    @Volatile
    private var cachedLnTen: BigDecimal? = null
    private suspend fun lnTen(): BigDecimal {
        cachedLnTen?.let { lnTen -> return lnTen }

        val result: BigDecimal = ln(TEN, workMathContext)
        cachedLnTen = result

        return result
    }

    @Volatile
    private var cachedOneE: BigDecimal? = null
    private suspend fun oneE(): BigDecimal {
        cachedOneE?.let { oneE -> return oneE }

        val result: BigDecimal = e(workMathContext)
        cachedOneE = result

        return result
    }

    private val tolerance: BigDecimal by lazy {
        ONE.scaleByPowerOfTen(-mathContext.precision + 2)
    }

    suspend fun e(): BigDecimal {
        return oneE()
            .round(mathContext)
    }

    suspend fun pi(): BigDecimal {
        return pi(workMathContext)
            .round(mathContext)
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
        return sqrt(value, workMathContext)
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

        val result: BigDecimal = if (integerExponent != null) {
            powIntegerExponent(base, integerExponent, workMathContext)
        } else {
            if (base.signum() < 0) {
                throw PowInvalidArgumentException()
            }
            powNonIntegerExponent(base, exponent, workMathContext)
        }

        return result
            .round(mathContext)
    }

    suspend fun ln(value: BigDecimal): BigDecimal {
        return ln(value, workMathContext)
            .round(mathContext)
    }

    suspend fun log(value: BigDecimal): BigDecimal {
        return ln(value, workMathContext)
            .divide(lnTen(), workMathContext)
            .round(mathContext)
    }

    suspend fun sin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return sin(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workMathContext)
            },
            workMathContext
        ).round(mathContext)
    }

    suspend fun cos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        return cos(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workMathContext)
            },
            workMathContext
        ).round(mathContext)
    }

    suspend fun tan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val cos: BigDecimal = cos(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workMathContext)
            },
            workMathContext
        )
        if (cos.signum() == 0) {
            throw TanInvalidArgumentException()
        }
        val sin: BigDecimal = sin(
            value = if (isRadians) {
                value
            } else {
                value.toRadians(workMathContext)
            },
            workMathContext
        )

        return sin.divide(cos, workMathContext)
            .round(mathContext)
    }

    suspend fun arcsin(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arcsin(value, workMathContext)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workMathContext)
        }.round(mathContext)
    }

    suspend fun arccos(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arccos(value, workMathContext)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workMathContext)
        }.round(mathContext)
    }

    suspend fun arctan(value: BigDecimal, isRadians: Boolean): BigDecimal {
        val result: BigDecimal = arctan(value, workMathContext)

        return if (isRadians) {
            result
        } else {
            result.toDegrees(workMathContext)
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
            .subtract(FOUR.multiply(arctanOneTwoHundredThirtyNine))
    }

    private suspend fun sqrt(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        fun initialSqrtEstimate(v: BigDecimal): BigDecimal {
            val orderOfMagnitude: Int = v.precision() - v.scale() - 1
            val sqrtExponent: Int = Math.floorDiv(orderOfMagnitude, 2)

            return BigDecimal.ONE.scaleByPowerOfTen(sqrtExponent)
        }

        if (value.signum() == -1) {
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

        val result: BigDecimal = base.pow(exponent.abs().intValueExact(), mc)

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

        val value: BigDecimal = exponent.multiply(ln(base, mc))

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

        val workingMc = MathContext(
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
                .divide(BigDecimal(n), workingMc)

            val nextSum: BigDecimal = sum.add(term).round(workingMc)

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

        var x: BigDecimal = value
        var exponent = 0

        while (x > sqrtTwo()) {
            coroutineContext.ensureActive()

            x = x.divide(TWO, mc)
            exponent++
        }

        while (x < inverseSqrtTwo()) {
            coroutineContext.ensureActive()

            x = x.multiply(TWO)
            exponent--
        }

        val lnX: BigDecimal = calculateLn(x, mc)

        return lnX.add(
            BigDecimal(exponent)
                .multiply(lnTwo())
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
        val angle: BigDecimal = normalizeAngle(value, mc)

        val pi: BigDecimal = pi(mc)
        val halfPi: BigDecimal = pi.divide(TWO)
        val twoPi: BigDecimal = pi.multiply(TWO)
        val threeHalfPi: BigDecimal = pi.add(halfPi)

        val distanceToZero: BigDecimal =
            minOf(angle, twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(threeHalfPi).abs()

        return when {
            distanceToZero <= tolerance || distanceToPi <= tolerance ->
                ZERO

            distanceToHalfPi <= tolerance ->
                ONE

            distanceToThreeHalfPi <= tolerance ->
                NEGATIVE_ONE

            angle <= halfPi ->
                sinTaylor(angle, mc)

            angle <= pi ->
                sinTaylor(pi.subtract(angle), mc)

            angle <= threeHalfPi ->
                sinTaylor(angle.subtract(pi), mc)
                    .negate()

            else ->
                sinTaylor(twoPi.subtract(angle), mc)
                    .negate()
        }
    }

    private suspend fun cos(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val angle: BigDecimal = normalizeAngle(value, mc)

        val pi: BigDecimal = pi(mc)
        val halfPi: BigDecimal = pi.divide(TWO)
        val twoPi: BigDecimal = pi.multiply(TWO)
        val threeHalfPi: BigDecimal = pi.add(halfPi)

        val distanceToZero: BigDecimal =
            minOf(angle, twoPi.subtract(angle).abs())
        val distanceToHalfPi: BigDecimal =
            angle.subtract(halfPi).abs()
        val distanceToPi: BigDecimal =
            angle.subtract(pi).abs()
        val distanceToThreeHalfPi: BigDecimal =
            angle.subtract(threeHalfPi).abs()

        return when {
            distanceToHalfPi <= tolerance || distanceToThreeHalfPi <= tolerance ->
                ZERO

            distanceToZero <= tolerance ->
                ONE

            distanceToPi <= tolerance ->
                NEGATIVE_ONE

            angle <= halfPi ->
                cosTaylor(angle, mc)

            angle <= pi ->
                cosTaylor(pi.subtract(angle), mc)
                    .negate()

            angle <= threeHalfPi ->
                cosTaylor(angle.subtract(pi), mc)
                    .negate()

            else ->
                cosTaylor(twoPi.subtract(angle), mc)
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
            return pi(mc).divide(TWO, mc)
        }

        if (value.compareTo(NEGATIVE_ONE) == 0) {
            return pi(mc).divide(TWO, mc).negate()
        }

        if (value.signum() == 0) {
            return ZERO
        }

        val absValue: BigDecimal = value.abs()
        val oneMinusXSquared: BigDecimal = ONE
            .subtract(absValue)
            .multiply(ONE.add(absValue))

        val root: BigDecimal = sqrt(oneMinusXSquared, mc)
        val result: BigDecimal = arctan(
            value.divide(root, mc),
            mc
        )

        return result
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
            return pi(mc)
        }

        if (value.signum() == 0) {
            return pi(mc).divide(TWO, mc)
        }

        if (value.signum() < 0) {
            return pi(mc).subtract(arccos(value.negate(), mc))
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
            return pi(mc).divide(TWO, mc)
                .subtract(
                    arctan(ONE.divide(value, mc), mc)
                )
        }

        if (value > ONE_HALF) {
            val reduced: BigDecimal = value.subtract(ONE)
                .divide(value.add(ONE), mc)

            return pi(mc).divide(FOUR, mc)
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
            .multiply(pi(mc))
            .divide(ONE_HUNDRED_EIGHTY, mc)
    }

    private suspend fun BigDecimal.toDegrees(
        mc: MathContext,
    ): BigDecimal {
        return this
            .multiply(ONE_HUNDRED_EIGHTY)
            .divide(pi(mc), mc)
    }

    private suspend fun normalizeAngle(
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        val twoPi: BigDecimal = pi(mc).multiply(TWO)
        var angle: BigDecimal = value.remainder(twoPi, mc)

        if (angle.signum() == -1) {
            angle = angle.add(twoPi)
        }

        return angle
    }
}

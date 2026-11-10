package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

enum class CalculatorError {
    Unsupported,
    ResultTooLarge,
    InsufficientPrecision,
    InvalidExpression,
    DivisionByZero,
    FactorialInvalidArgument,
    FactorialLargeArgument,
    SqrtInvalidArgument,
    ExpLargeArgument,
    PowInvalidArgument,
    PowLimitExceeded,
    LogInvalidArgument,
    TanInvalidArgument,
    ArcsinInvalidArgument,
    ArccosInvalidArgument,
    Arithmetic,
}

private class InvalidNumericFormatException : IllegalArgumentException()
private class MissingFactorException : IllegalArgumentException()
private class IllegalFactorException : IllegalArgumentException()
private class FactorialLargeArgumentException : IllegalArgumentException()
private class FactorialInvalidArgumentException : ArithmeticException()
private class ExpLargeArgumentException : IllegalArgumentException()
private class PowInvalidArgumentException : ArithmeticException()
private class PowLimitExceededException : IllegalArgumentException()

private val MAX_EXPONENTIAL_ARGUMENT: BigDecimal =
    Constraints.MAX_EXPONENTIAL_ARGUMENT.toBigDecimal()
private const val MAX_FACTORIAL_ARGUMENT: Int = Constraints.MAX_FACTORIAL_ARGUMENT
private const val MAX_INTEGER_EXPONENT: Int = Constraints.MAX_INTEGER_EXPONENT
private const val MAX_NON_INTEGER_EXPONENT: Int = Constraints.MAX_NON_INTEGER_EXPONENT
private const val TOLERANCE_PRECISION_MARGIN: Int = 1
private const val ADDITIONAL_PRECISION_PER_FUNCTION: Int = 2
private val SymbolizerError.calculatorError: CalculatorError
    get() = when (this) {
        SymbolizerError.Unsupported -> CalculatorError.Unsupported
        SymbolizerError.TooLarge -> CalculatorError.ResultTooLarge
    }

suspend fun calculate(
    expression: List<Symbol>,
    isRadians: Boolean,
    precision: Int,
): Result<List<Symbol>, CalculatorError> {
    require(precision > 0)

    val calculated: BigDecimal =
        when (val calculatedResult: Result<BigDecimal, CalculatorError> =
            parse(
                expression = expression,
                isRadians = isRadians,
                precision = precision,
            )) {
            is Result.Ok -> calculatedResult.value
            is Result.Err -> return Result.Err(calculatedResult.error)
        }

    return when (val calculatedSymbolsResult: Result<List<Symbol>, SymbolizerError> =
        calculated.toSymbols()) {
        is Result.Ok -> Result.Ok(calculatedSymbolsResult.value)
        is Result.Err -> Result.Err(calculatedSymbolsResult.error.calculatorError)
    }
}

private suspend fun parse(
    expression: List<Symbol>,
    isRadians: Boolean,
    precision: Int,
): Result<BigDecimal, CalculatorError> {
    if (expression.isEmpty()) {
        return Result.Err(CalculatorError.InvalidExpression)
    }

    val tolerancePrecision: Int = -precision + TOLERANCE_PRECISION_MARGIN
    if (tolerancePrecision >= 0) {
        return Result.Err(CalculatorError.InsufficientPrecision)
    }

    return try {
        val parser = Parser(
            expression = expression,
            isRadians = isRadians,
            tolerance = BigDecimal.ONE.scaleByPowerOfTen(tolerancePrecision),
            additionalPrecision = ADDITIONAL_PRECISION_PER_FUNCTION,
        )
        val mc = MathContext(
            precision,
            RoundingMode.HALF_UP,
        )
        val result: BigDecimal = parser
            .parseExpression(mc)
            .toBigDecimal(mc)

        if (parser.isEnd()) {
            Result.Ok(result)
        } else {
            Result.Err(CalculatorError.InvalidExpression)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: InvalidNumericFormatException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: MissingFactorException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: IllegalFactorException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: IllegalArgumentException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: FactorialLargeArgumentException) {
        Result.Err(CalculatorError.FactorialLargeArgument)
    } catch (_: FactorialInvalidArgumentException) {
        Result.Err(CalculatorError.FactorialInvalidArgument)
    } catch (_: ExpLargeArgumentException) {
        Result.Err(CalculatorError.ExpLargeArgument)
    } catch (_: PowInvalidArgumentException) {
        Result.Err(CalculatorError.PowInvalidArgument)
    } catch (_: PowLimitExceededException) {
        Result.Err(CalculatorError.PowLimitExceeded)
    } catch (_: DivisionByZeroException) {
        Result.Err(CalculatorError.DivisionByZero)
    } catch (_: SqrtInvalidArgumentException) {
        Result.Err(CalculatorError.SqrtInvalidArgument)
    } catch (_: LogInvalidArgumentException) {
        Result.Err(CalculatorError.LogInvalidArgument)
    } catch (_: TanInvalidArgumentException) {
        Result.Err(CalculatorError.TanInvalidArgument)
    } catch (_: ArcsinInvalidArgumentException) {
        Result.Err(CalculatorError.ArcsinInvalidArgument)
    } catch (_: ArccosInvalidArgumentException) {
        Result.Err(CalculatorError.ArccosInvalidArgument)
    } catch (_: ArithmeticException) {
        Result.Err(CalculatorError.Arithmetic)
    } catch (_: IllegalArgumentException) {
        Result.Err(CalculatorError.InvalidExpression)
    }
}

private class Parser(
    private val expression: List<Symbol>,
    private val isRadians: Boolean,
    private val tolerance: BigDecimal,
    private val additionalPrecision: Int,
) {
    private var position: Int = 0

    fun isEnd(): Boolean = (position >= expression.size)

    suspend fun parseExpression(
        mc: MathContext
    ): NumericValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: NumericValue = parseTerm(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (expression[position]) {
                is Symbol.Operator.Add -> {
                    position++
                    value + parseTerm(mc)
                }

                is Symbol.Operator.Subtract -> {
                    position++
                    value - parseTerm(mc)
                }

                else ->
                    break
            }
        }
        return value
    }

    private suspend fun parseTerm(
        mc: MathContext,
    ): NumericValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: NumericValue = parseSign(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (expression[position]) {
                is Symbol.Operator.Multiply -> {
                    position++
                    value * parseSign(mc)
                }

                is Symbol.Operator.Divide -> {
                    position++
                    value / parseSign(mc)
                }

                else ->
                    break
            }
        }
        return value
    }

    private suspend fun parseSign(
        mc: MathContext,
    ): NumericValue {
        return when (expression.getOrNull(position)) {
            Symbol.Sign.Positive -> {
                position++
                parseSign(mc)
            }

            Symbol.Sign.Negative -> {
                position++
                parseSign(mc).negate()
            }

            else ->
                parseBinaryOperation(mc)
        }
    }

    private suspend fun parseBinaryOperation(
        mc: MathContext,
    ): NumericValue {
        val base: NumericValue = parsePostfixOperation(mc)

        return if (expression.getOrNull(position) is Symbol.Operator.Power) {
            position++
            pow(
                base = base,
                exponent = parseSign(increasePrecision(mc)),
                mc = mc,
            )
        } else {
            base
        }
    }

    private suspend fun parsePostfixOperation(
        mc: MathContext,
    ): NumericValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: NumericValue = parseFactor(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            when (expression[position]) {
                is Symbol.PostfixOperator.Factorial -> {
                    position++
                    value = factorial(value)
                }

                else ->
                    break
            }
        }

        return value
    }

    private suspend fun parseFactor(
        mc: MathContext,
    ): NumericValue {
        if (isEnd()) {
            throw MissingFactorException()
        }

        return when (val symbol: Symbol = expression[position]) {
            is Symbol.Numeric ->
                parseNumeric()

            is Symbol.Constant -> {
                position++
                parseConstant(symbol, increasePrecision(mc))
            }

            is Symbol.Factor.Function -> {
                position++
                val workingMc: MathContext = increasePrecision(mc)
                applyFunction(
                    function = symbol,
                    value = parseSign(workingMc),
                    mc = workingMc,
                )
            }

            is Symbol.FactorStart.OpeningParenthesis -> {
                position++
                parseFactorBody(mc)
            }

            is Symbol.FactorStart.Function -> {
                position++
                val workingMc: MathContext = increasePrecision(mc)
                applyFunction(
                    function = symbol,
                    value = parseFactorBody(workingMc),
                    mc = workingMc,
                )
            }

            else ->
                throw MissingFactorException()
        }
    }

    private suspend fun parseFactorBody(
        mc: MathContext,
    ): NumericValue {
        val value: NumericValue = parseExpression(mc)

        if (isEnd() || expression[position] !is Symbol.FactorEnd
        ) {
            throw IllegalFactorException()
        }
        position++

        return value
    }

    private suspend fun parseNumeric(): NumericValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val buffer = StringBuilder()

        while (!isEnd()) {
            coroutineContext.ensureActive()

            when (val symbol: Symbol = expression[position]) {
                is Symbol.Numeric.Digit ->
                    buffer.append(symbol.value)

                is Symbol.Numeric.Point ->
                    buffer.append('.')

                else ->
                    break
            }
            position++
        }

        val result: BigDecimal = buffer.toString().toBigDecimalOrNull()
            ?: throw InvalidNumericFormatException()

        return NumericValue.from(result)
    }

    private suspend fun parseConstant(
        constant: Symbol.Constant,
        mc: MathContext,
    ): NumericValue {
        val result: BigDecimal = when (constant) {
            Symbol.Constant.Pi ->
                BigDecimalMath.pi(
                    mc = mc,
                )

            Symbol.Constant.Euler ->
                BigDecimalMath.e(
                    mc = mc,
                )
        }

        return NumericValue.from(result)
    }

    private suspend fun applyFunction(
        function: Symbol.Factor.Function,
        value: NumericValue,
        mc: MathContext,
    ): NumericValue {
        return when (function) {
            Symbol.Factor.Function.Sqrt ->
                NumericValue.from(
                    BigDecimalMath.sqrt(
                        value = value.toBigDecimal(mc),
                        mc = mc,
                    )
                )
        }
    }

    private suspend fun applyFunction(
        function: Symbol.FactorStart.Function,
        value: NumericValue,
        mc: MathContext,
    ): NumericValue {
        val bigDecimalValue = value.toBigDecimal(mc)

        val result: BigDecimal = when (function) {
            Symbol.FactorStart.Function.Exp -> {
                if (bigDecimalValue.abs() > MAX_EXPONENTIAL_ARGUMENT) {
                    throw ExpLargeArgumentException()
                }

                BigDecimalMath.exp(
                    value = bigDecimalValue,
                    mc = mc,
                )
            }

            Symbol.FactorStart.Function.Ln ->
                BigDecimalMath.ln(
                    value = bigDecimalValue,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Log ->
                BigDecimalMath.log(
                    value = bigDecimalValue,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Sin ->
                BigDecimalMath.sin(
                    angle = bigDecimalValue,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Cos ->
                BigDecimalMath.cos(
                    angle = bigDecimalValue,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Tan ->
                BigDecimalMath.tan(
                    angle = bigDecimalValue,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arcsin ->
                BigDecimalMath.arcsin(
                    value = bigDecimalValue,
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arccos ->
                BigDecimalMath.arccos(
                    value = bigDecimalValue,
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arctan ->
                BigDecimalMath.arctan(
                    value = bigDecimalValue,
                    isRadians = isRadians,
                    mc = mc,
                )
        }

        return NumericValue.from(result)
    }

    private suspend fun factorial(
        value: NumericValue
    ): NumericValue {
        return NumericValue.from(
            BigDecimalMath.factorial(
                value = factorialArgument(value)
            )
        )
    }

    private suspend fun pow(
        base: NumericValue,
        exponent: NumericValue,
        mc: MathContext,
    ): NumericValue {
        if (base.signum() == 0) {
            if (exponent.signum() > 0) {
                return NumericValue.ZERO
            }
            throw PowInvalidArgumentException()
        }

        if (exponent.signum() == 0) {
            return NumericValue.ONE
        }

        val integerExponent: Int? = integerExponent(exponent)

        return if (integerExponent != null) {
            if (base.decimal.signum() == 0) {
                powRationalBase(
                    base = base,
                    exponent = integerExponent,
                )
            } else {
                powNaturalExponent(
                    base = base.toBigDecimal(increasePrecision(mc)),
                    exponent = integerExponent,
                    mc = mc,
                )
            }
        } else {
            powDecimalExponent(
                base = base.toBigDecimal(increasePrecision(mc)),
                exponent = exponent,
                mc = mc,
            )
        }
    }

    private suspend fun powRationalBase(
        base: NumericValue,
        exponent: Int,
    ): NumericValue {
        return NumericValue.of(
            rational = base.rational.pow(exponent)
        )
    }

    private suspend fun powNaturalExponent(
        base: BigDecimal,
        exponent: Int,
        mc: MathContext,
    ): NumericValue {
        val result: BigDecimal = BigDecimalMath.powNaturalExponent(
            base = base,
            exponent = abs(exponent),
        )

        return NumericValue.from(
            if (exponent >= 0) {
                result
            } else {
                BigDecimal.ONE.divide(result, increasePrecision(mc))

            }
        )
    }

    private suspend fun powDecimalExponent(
        base: BigDecimal,
        exponent: NumericValue,
        mc: MathContext,
    ): NumericValue {
        if (base.signum() < 0) {
            throw PowInvalidArgumentException()
        }

        return NumericValue.from(
            BigDecimalMath.powDecimalExponent(
                base = base,
                exponent = nonIntegerExponent(exponent, increasePrecision(mc)),
                mc = mc,
            )
        )
    }

    private fun factorialArgument(
        value: NumericValue,
    ): Int {
        val bigIntegerArgument: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            throw FactorialInvalidArgumentException()
        }

        if (bigIntegerArgument.signum() < 0) {
            throw FactorialInvalidArgumentException()
        }

        val argument: Int = try {
            bigIntegerArgument.intValueExact()
        } catch (_: ArithmeticException) {
            throw FactorialLargeArgumentException()
        }

        if (argument > MAX_FACTORIAL_ARGUMENT) {
            throw FactorialLargeArgumentException()
        }

        return argument
    }

    private fun integerExponent(
        value: NumericValue,
    ): Int? {
        val bigIntegerExponent: BigInteger = try {
            value.toBigIntegerExact()
        } catch (_: ArithmeticException) {
            return null
        }

        val exponent: Int = try {
            bigIntegerExponent.intValueExact()
        } catch (_: ArithmeticException) {
            throw PowLimitExceededException()
        }

        if (exponent !in -MAX_INTEGER_EXPONENT..MAX_INTEGER_EXPONENT) {
            throw PowLimitExceededException()
        }

        return exponent
    }

    private fun nonIntegerExponent(
        value: NumericValue,
        mc: MathContext,
    ): BigDecimal {
        val exponent: BigDecimal = value.toBigDecimal(mc)

        if (exponent.abs() > MAX_NON_INTEGER_EXPONENT.toBigDecimal()) {
            throw PowLimitExceededException()
        }

        return exponent
    }

    private fun increasePrecision(
        mc: MathContext,
    ): MathContext {
        return MathContext(
            mc.precision + additionalPrecision,
            mc.roundingMode,
        )
    }
}

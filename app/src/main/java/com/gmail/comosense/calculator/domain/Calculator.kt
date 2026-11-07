package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.coroutines.CoroutineContext

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
private class DivisionByZeroException : ArithmeticException()

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
    } catch (_: DivisionByZeroException) {
        Result.Err(CalculatorError.DivisionByZero)
    } catch (_: FactorialInvalidArgumentException) {
        Result.Err(CalculatorError.FactorialInvalidArgument)
    } catch (_: FactorialLargeArgumentException) {
        Result.Err(CalculatorError.FactorialLargeArgument)
    } catch (_: SqrtInvalidArgumentException) {
        Result.Err(CalculatorError.SqrtInvalidArgument)
    } catch (_: ExpLargeArgumentException) {
        Result.Err(CalculatorError.ExpLargeArgument)
    } catch (_: PowInvalidArgumentException) {
        Result.Err(CalculatorError.PowInvalidArgument)
    } catch (_: PowLimitExceededException) {
        Result.Err(CalculatorError.PowLimitExceeded)
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
    ): Rational {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: Rational = parseTerm(mc)

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
    ): Rational {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: Rational = parseSign(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (expression[position]) {
                is Symbol.Operator.Multiply -> {
                    position++
                    value * parseSign(mc)
                }

                is Symbol.Operator.Divide -> {
                    position++
                    val divisor = parseSign(mc)

                    if (divisor.isZero()) {
                        throw DivisionByZeroException()
                    }

                    value / divisor
                }

                else ->
                    break
            }
        }
        return value
    }

    private suspend fun parseSign(
        mc: MathContext,
    ): Rational {
        return when (expression.getOrNull(position)) {
            Symbol.Sign.Positive -> {
                position++
                parseSign(mc)
            }

            Symbol.Sign.Negative -> {
                position++
                -parseSign(mc)
            }

            else ->
                parseBinaryOperation(mc)
        }
    }

    private suspend fun parseBinaryOperation(
        mc: MathContext,
    ): Rational {
        val base: Rational = parsePostfixOperation(mc)

        return if (expression.getOrNull(position)
                    is Symbol.Operator.Power
        ) {
            position++
            Rational.of(
                BigDecimalMath.pow(
                    base = base
                        .toBigDecimal(increasePrecision(mc)),
                    exponent = parseSign(increasePrecision(mc))
                        .toBigDecimal(increasePrecision(mc)),
                    mc = mc,
                )
            )
        } else {
            base
        }
    }

    private suspend fun parsePostfixOperation(
        mc: MathContext,
    ): Rational {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: Rational = parseFactor(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            when (expression[position]) {
                is Symbol.PostfixOperator.Factorial -> {
                    position++
                    value = Rational.of(
                        BigDecimalMath.factorial(
                            value = value.toBigDecimal(mc),
                        )
                    )
                }

                else ->
                    break
            }
        }

        return value
    }

    private suspend fun parseFactor(
        mc: MathContext,
    ): Rational {
        if (isEnd()) {
            throw MissingFactorException()
        }

        return when (val symbol: Symbol = expression[position]) {
            is Symbol.Numeric ->
                Rational.of(
                    parseNumeric()
                )

            is Symbol.Constant -> {
                position++
                Rational.of(
                    parseConstant(symbol, increasePrecision(mc))
                )
            }

            is Symbol.Factor.Function -> {
                position++
                applyFunction(
                    function = symbol,
                    value = parseSign(increasePrecision(mc))
                        .toBigDecimal(increasePrecision(mc)),
                    mc = increasePrecision(mc),
                )
            }

            is Symbol.FactorStart.OpeningParenthesis -> {
                position++
                parseFactorBody(mc)
            }

            is Symbol.FactorStart.Function -> {
                position++
                applyFunction(
                    function = symbol,
                    value = parseFactorBody(increasePrecision(mc))
                        .toBigDecimal(increasePrecision(mc)),
                    mc = increasePrecision(mc),
                )
            }

            else ->
                throw MissingFactorException()
        }
    }

    private suspend fun parseFactorBody(
        mc: MathContext,
    ): Rational {
        val value: Rational = parseExpression(mc)

        if (isEnd() || expression[position] !is Symbol.FactorEnd
        ) {
            throw IllegalFactorException()
        }
        position++

        return value
    }

    private suspend fun parseNumeric(): BigDecimal {
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

        return buffer.toString().toBigDecimalOrNull()
            ?: throw InvalidNumericFormatException()
    }

    private suspend fun parseConstant(
        constant: Symbol.Constant,
        mc: MathContext,
    ): BigDecimal {
        return when (constant) {
            Symbol.Constant.Pi ->
                BigDecimalMath.pi(
                    mc = mc,
                )

            Symbol.Constant.Euler ->
                BigDecimalMath.e(
                    mc = mc,
                )
        }
    }

    private suspend fun applyFunction(
        function: Symbol.Factor.Function,
        value: BigDecimal,
        mc: MathContext,
    ): Rational {
        return when (function) {
            Symbol.Factor.Function.Sqrt ->
                Rational.of(
                    BigDecimalMath.sqrt(
                        value = value,
                        mc = mc,
                    )
                )
        }
    }

    private suspend fun applyFunction(
        function: Symbol.FactorStart.Function,
        value: BigDecimal,
        mc: MathContext,
    ): Rational {
        val result: BigDecimal = when (function) {
            Symbol.FactorStart.Function.Exp ->
                BigDecimalMath.exp(
                    value = value,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Ln ->
                BigDecimalMath.ln(
                    value = value,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Log ->
                BigDecimalMath.log(
                    value = value,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Sin ->
                BigDecimalMath.sin(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Cos ->
                BigDecimalMath.cos(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Tan ->
                BigDecimalMath.tan(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arcsin ->
                BigDecimalMath.arcsin(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arccos ->
                BigDecimalMath.arccos(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arctan ->
                BigDecimalMath.arctan(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )
        }

        return Rational.of(result)
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

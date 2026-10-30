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
    val tokens: List<Token> = expression.tokenize()
        ?: return Result.Err(CalculatorError.InvalidExpression)

    val calculated: BigDecimal =
        when (val calculatedResult: Result<BigDecimal, CalculatorError> =
            parse(
                expression = tokens,
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
    expression: List<Token>,
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
            tokens = expression,
            isRadians = isRadians,
            tolerance = BigDecimal.ONE.scaleByPowerOfTen(tolerancePrecision),
            additionalPrecision = ADDITIONAL_PRECISION_PER_FUNCTION,
        )
        val mc = MathContext(
            precision,
            RoundingMode.HALF_UP,
        )
        val result: BigDecimal = parser.parseExpression(mc)

        if (parser.isEnd()) {
            Result.Ok(result)
        } else {
            Result.Err(CalculatorError.InvalidExpression)
        }
    } catch (e: CancellationException) {
        throw e
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
    private val tokens: List<Token>,
    private val isRadians: Boolean,
    private val tolerance: BigDecimal,
    private val additionalPrecision: Int,
) {
    private var position: Int = 0

    fun isEnd(): Boolean = (position >= tokens.size)

    suspend fun parseExpression(
        mc: MathContext
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: BigDecimal = parseTerm(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (tokens[position]) {
                is Token.Operator.Add -> {
                    position++
                    value.add(parseTerm(mc))
                }

                is Token.Operator.Subtract -> {
                    position++
                    value.subtract(parseTerm(mc))
                }

                else ->
                    return value
            }
        }
        return value
    }

    private suspend fun parseTerm(
        mc: MathContext,
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: BigDecimal = parseSign(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (tokens[position]) {
                is Token.Operator.Multiply -> {
                    position++
                    value.multiply(parseSign(mc))
                }

                is Token.Operator.Divide -> {
                    position++
                    val divisor: BigDecimal = parseSign(mc)
                    if (divisor.signum() == 0) {
                        throw DivisionByZeroException()
                    }
                    value.divide(divisor, mc)
                }

                else ->
                    return value
            }
        }
        return value
    }

    private suspend fun parseSign(
        mc: MathContext,
    ): BigDecimal {
        return when (tokens.getOrNull(position)) {
            Token.Sign.Positive -> {
                position++
                parseSign(mc)
            }

            Token.Sign.Negative -> {
                position++
                parseSign(mc).negate()
            }

            else ->
                parseBinaryOperation(mc)
        }
    }

    private suspend fun parseBinaryOperation(
        mc: MathContext,
    ): BigDecimal {
        val base: BigDecimal = parsePostfixOperation(mc)

        return if (tokens.getOrNull(position) is Token.Operator.Power) {
            position++
            BigDecimalMath.pow(
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
    ): BigDecimal {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: BigDecimal = parseFactor(mc)

        while (tokens.getOrNull(position) is Token.PostfixOperator.Factorial) {
            coroutineContext.ensureActive()

            position++
            value = BigDecimalMath.factorial(
                value = value,
            )
        }

        return value
    }

    private suspend fun parseFactor(
        mc: MathContext,
    ): BigDecimal {
        if (isEnd()) {
            throw MissingFactorException()
        }

        return when (val token: Token = tokens[position]) {
            is Token.Numeric -> {
                position++
                token.value
            }

            is Token.Constant -> {
                position++
                parseConstant(token, mc)
            }

            is Token.Factor.Function -> {
                position++
                applyFunction(
                    function = token,
                    value = parseSign(increasePrecision(mc)),
                    mc = mc,
                )
            }

            is Token.FactorStart.OpeningParenthesis -> {
                position++
                parseFactorBody(mc)
            }

            is Token.FactorStart.Function -> {
                position++
                applyFunction(
                    function = token,
                    value = parseFactorBody(increasePrecision(mc)),
                    mc = mc,
                )
            }

            else ->
                throw MissingFactorException()
        }
    }

    private suspend fun parseFactorBody(
        mc: MathContext,
    ): BigDecimal {
        val value: BigDecimal = parseExpression(mc)
        if (isEnd() || tokens[position] !is Token.FactorEnd) {
            throw IllegalFactorException()
        }

        position++
        return value
    }

    private suspend fun parseConstant(
        constant: Token.Constant,
        mc: MathContext,
    ): BigDecimal {
        return when (constant) {
            Token.Constant.Pi ->
                BigDecimalMath.pi(
                    mc = mc,
                )

            Token.Constant.Euler ->
                BigDecimalMath.e(
                    mc = mc,
                )
        }
    }

    private suspend fun applyFunction(
        function: Token.Factor.Function,
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return when (function) {
            Token.Factor.Function.Sqrt ->
                BigDecimalMath.sqrt(
                    value = value,
                    mc = mc,
                )
        }
    }

    private suspend fun applyFunction(
        function: Token.FactorStart.Function,
        value: BigDecimal,
        mc: MathContext,
    ): BigDecimal {
        return when (function) {
            Token.FactorStart.Function.Exp ->
                BigDecimalMath.exp(
                    value = value,
                    mc = mc,
                )

            Token.FactorStart.Function.Ln ->
                BigDecimalMath.ln(
                    value = value,
                    mc = mc,
                )

            Token.FactorStart.Function.Log ->
                BigDecimalMath.log(
                    value = value,
                    mc = mc,
                )

            Token.FactorStart.Function.Sin ->
                BigDecimalMath.sin(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Token.FactorStart.Function.Cos ->
                BigDecimalMath.cos(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Token.FactorStart.Function.Tan ->
                BigDecimalMath.tan(
                    angle = value,
                    isRadians = isRadians,
                    tolerance = tolerance,
                    mc = mc,
                )

            Token.FactorStart.Function.Arcsin ->
                BigDecimalMath.arcsin(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )

            Token.FactorStart.Function.Arccos ->
                BigDecimalMath.arccos(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )

            Token.FactorStart.Function.Arctan ->
                BigDecimalMath.arctan(
                    value = value,
                    isRadians = isRadians,
                    mc = mc,
                )
        }
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

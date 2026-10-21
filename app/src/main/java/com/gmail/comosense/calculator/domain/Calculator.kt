package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalculatorError {
    Unsupported,
    ResultTooLarge,
    InvalidExpression,
    DivisionByZero,
    FactorialInvalidArgument,
    FactorialLargeArgument,
    SqrtInvalidArgument,
    PowInvalidArgument,
    PowLimitExceeded,
    LogInvalidArgument,
    TanInvalidArgument,
    Arithmetic,
}

private class DivisionByZeroException : ArithmeticException()

private val SymbolizerError.calculatorError: CalculatorError
    get() = when (this) {
        SymbolizerError.Unsupported -> CalculatorError.Unsupported
        SymbolizerError.TooLarge -> CalculatorError.ResultTooLarge
    }

fun calculate(
    expression: List<Symbol>,
    isRadians: Boolean,
    precision: Int,
): Result<List<Symbol>, CalculatorError> {
    require(precision > 0)
    val tokens: List<Token> = tokenize(expression)
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

private fun parse(
    expression: List<Token>,
    isRadians: Boolean,
    precision: Int,
): Result<BigDecimal, CalculatorError> {
    if (expression.isEmpty()) {
        return Result.Err(CalculatorError.InvalidExpression)
    }

    return try {
        val parser = Parser(
            tokens = expression,
            isRadians = isRadians,
            precision = precision,
        )
        val result: BigDecimal = parser.parseExpression()

        if (parser.isEnd()) {
            Result.Ok(result)
        } else {
            Result.Err(CalculatorError.InvalidExpression)
        }
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
    } catch (_: PowInvalidArgumentException) {
        Result.Err(CalculatorError.PowInvalidArgument)
    } catch (_: PowLimitExceededException) {
        Result.Err(CalculatorError.PowLimitExceeded)
    } catch (_: LogInvalidArgumentException) {
        Result.Err(CalculatorError.LogInvalidArgument)
    } catch (_: TanInvalidArgumentException) {
        Result.Err(CalculatorError.TanInvalidArgument)
    } catch (_: ArithmeticException) {
        Result.Err(CalculatorError.Arithmetic)
    }
}

private class Parser(
    private val tokens: List<Token>,
    private val isRadians: Boolean,
    precision: Int,
) {
    private val mathContext: MathContext = MathContext(
        precision,
        RoundingMode.HALF_UP,
    )
    private val bigDecimalMath = BigDecimalMath(mathContext)

    private var position: Int = 0

    fun isEnd(): Boolean = (position >= tokens.size)

    fun parseExpression(): BigDecimal {
        var value: BigDecimal = parseTerm()

        while (!isEnd()) {
            value = when (tokens[position]) {
                is Token.Operator.Add -> {
                    position++
                    value.add(parseTerm())
                }

                is Token.Operator.Subtract -> {
                    position++
                    value.subtract(parseTerm())
                }

                else -> {
                    return value
                }
            }
        }
        return value
    }

    private fun parseTerm(): BigDecimal {
        var value: BigDecimal = parseSign()

        while (!isEnd()) {
            value = when (tokens[position]) {
                is Token.Operator.Multiply -> {
                    position++
                    value.multiply(parseSign())
                }

                is Token.Operator.Divide -> {
                    position++
                    val divisor: BigDecimal = parseSign()
                    if (divisor.signum() == 0) {
                        throw DivisionByZeroException()
                    }
                    value.divide(divisor, mathContext)
                }

                else -> {
                    return value
                }
            }
        }
        return value
    }

    private fun parseSign(): BigDecimal {
        return when (tokens.getOrNull(position)) {
            Token.Sign.Positive -> {
                position++
                parseSign()
            }

            Token.Sign.Negative -> {
                position++
                parseSign().negate()
            }

            else -> {
                parseBinaryOperation()
            }
        }
    }

    private fun parseBinaryOperation(): BigDecimal {
        val base: BigDecimal = parsePostfixOperation()

        return if (tokens.getOrNull(position) is Token.SpecialOperator.Power) {
            position++
            bigDecimalMath.pow(base, parseSign())
        } else {
            base
        }
    }

    private fun parsePostfixOperation(): BigDecimal {
        var value: BigDecimal = parseFactor()

        while (tokens.getOrNull(position) is Token.SpecialOperator.Factorial) {
            position++
            value = bigDecimalMath.factorial(value)
        }

        return value
    }

    private fun parseFactor(): BigDecimal {
        if (isEnd()) {
            throw IllegalArgumentException("Expected factor")
        }

        return when (val token: Token = tokens[position]) {
            is Token.Numeric -> {
                position++
                token.value
            }

            is Token.Constant -> {
                position++
                parseConstant(token)
            }

            is Token.Factor.Function -> {
                position++
                applyFunction(token, parseFactor())
            }

            is Token.FactorStart.OpeningParenthesis -> {
                position++
                parseFactorBody()
            }

            is Token.FactorStart.Function -> {
                position++
                applyFunction(token, parseFactorBody())
            }

            else -> {
                throw IllegalArgumentException("Expected factor")
            }
        }
    }

    private fun parseFactorBody(): BigDecimal {
        val value: BigDecimal = parseExpression()
        if (isEnd() || tokens[position] !is Token.FactorEnd) {
            throw IllegalArgumentException("Missing FactorEnd")
        }

        position++
        return value
    }

    private fun parseConstant(constant: Token.Constant): BigDecimal {
        return when (constant) {
            Token.Constant.Pi ->
                bigDecimalMath.pi()

            Token.Constant.Euler ->
                bigDecimalMath.e()
        }
    }

    private fun applyFunction(
        function: Token.Factor.Function,
        value: BigDecimal,
    ): BigDecimal {
        return when (function) {
            Token.Factor.Function.Sqrt ->
                bigDecimalMath.sqrt(value)
        }
    }

    private fun applyFunction(
        function: Token.FactorStart.Function,
        value: BigDecimal,
    ): BigDecimal {
        return when (function) {
            Token.FactorStart.Function.Sin ->
                bigDecimalMath.sin(value, isRadians)

            Token.FactorStart.Function.Cos ->
                bigDecimalMath.cos(value, isRadians)

            Token.FactorStart.Function.Tan ->
                bigDecimalMath.tan(value, isRadians)

            Token.FactorStart.Function.Log ->
                bigDecimalMath.log(value)

            Token.FactorStart.Function.Ln ->
                bigDecimalMath.ln(value)
        }
    }
}

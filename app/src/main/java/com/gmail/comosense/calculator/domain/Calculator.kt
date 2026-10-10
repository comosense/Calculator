package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalculatorError {
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
    Unsupported,
}

private class DivisionByZeroException : ArithmeticException()

fun calculate(
    expression: List<Symbol>,
    precision: Int,
    displayScale: Int,
): Result<List<Symbol>, CalculatorError> {
    require(precision > 0)
    require(displayScale >= 0)

    val tokens: List<Token> =
        when (val tokensResult: Result<List<Token>, SymbolTokenizerError> = tokenize(expression)) {
            is Result.Ok ->
                tokensResult.value

            is Result.Err ->
                return Result.Err(CalculatorError.InvalidExpression)
        }

    return when (val calculatedResult: Result<BigDecimal, CalculatorError> =
        calculate(tokens, precision)) {
        is Result.Ok ->
            calculatedResult.value.toSymbols(displayScale)

        is Result.Err ->
            calculatedResult
    }
}

private fun calculate(
    tokens: List<Token>,
    precision: Int,
): Result<BigDecimal, CalculatorError> {
    if (tokens.isEmpty()) {
        return Result.Err(CalculatorError.InvalidExpression)
    }

    return try {
        val parser = Parser(tokens, precision)
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
                    value.add(parseTerm(), mathContext)
                }

                is Token.Operator.Subtract -> {
                    position++
                    value.subtract(parseTerm(), mathContext)
                }

                else -> return value
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
                    value.multiply(parseSign(), mathContext)
                }

                is Token.Operator.Divide -> {
                    position++
                    val divisor: BigDecimal = parseSign()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw DivisionByZeroException()
                    }
                    value.divide(divisor, mathContext)
                }

                else -> return value
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
                parseSign().negate(mathContext)
            }

            else -> parsePower()
        }
    }

    private fun parsePower(): BigDecimal {
        val base: BigDecimal = parseFactorial()

        return if (tokens.getOrNull(position) is Token.SpecialOperator.Power) {
            position++
            bigDecimalMath.pow(base, parseSign())
        } else {
            base
        }
    }

    private fun parseFactorial(): BigDecimal {
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
                bigDecimalMath.sin(value)

            Token.FactorStart.Function.Cos ->
                bigDecimalMath.cos(value)

            Token.FactorStart.Function.Tan ->
                bigDecimalMath.tan(value)

            Token.FactorStart.Function.Log ->
                bigDecimalMath.log(value)

            Token.FactorStart.Function.Ln ->
                bigDecimalMath.ln(value)
        }
    }
}

private fun BigDecimal.toSymbols(scale: Int): Result<List<Symbol>, CalculatorError> {
    val displayResult: BigDecimal = if (compareTo(BigDecimal.ZERO) == 0) {
        BigDecimal.ZERO
    } else {
        setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    val symbols: List<Symbol> = buildList {
        for (c in displayResult.toPlainString()) {
            add(
                when (c) {
                    in '0'..'9' -> Symbol.Numeric.Digit(c.digitToInt())
                    '.' -> Symbol.Numeric.Point
                    '-' -> Symbol.Sign.Negative
                    else -> return Result.Err(CalculatorError.Unsupported)
                }
            )
        }
    }

    return Result.Ok(symbols)
}

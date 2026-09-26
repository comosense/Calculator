package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalculatorError {
    InvalidExpression,
    DivisionByZero,
    Arithmetic,
    Unsupposed,
}

private class DivisionByZeroException : ArithmeticException()

fun calculate(
    expression: List<Symbol>,
    precision: Int,
    displayScale: Int,
): Result<List<Symbol>, CalculatorError> {
    return when (val tokensResult: Result<List<Token>, SymbolParserError> =
        parseTokens(expression)) {
        is Result.Ok -> {
            when (val calculatedResult: Result<BigDecimal, CalculatorError> =
                calculate(tokensResult.value, precision)) {
                is Result.Ok -> {
                    when (val symbolsResult: Result<List<Symbol>, CalculatorError> =
                        calculatedResult.value.toSymbols(displayScale)) {
                        is Result.Ok -> {
                            Result.Ok(symbolsResult.value)
                        }

                        is Result.Err -> {
                            symbolsResult
                        }
                    }
                }

                is Result.Err -> {
                    calculatedResult
                }
            }
        }

        is Result.Err -> {
            Result.Err(CalculatorError.InvalidExpression)
        }
    }
}

private fun calculate(
    expression: List<Token>,
    precision: Int,
): Result<BigDecimal, CalculatorError> {
    if (expression.isEmpty()) {
        return Result.Err(CalculatorError.InvalidExpression)
    }

    return try {
        val parser = Parser(expression, precision)
        val result: BigDecimal = parser.parseExpression()

        if (parser.isEnd()) {
            Result.Ok(result)
        } else {
            Result.Err(CalculatorError.InvalidExpression)
        }
    } catch (_: DivisionByZeroException) {
        Result.Err(CalculatorError.DivisionByZero)
    } catch (_: IllegalArgumentException) {
        Result.Err(CalculatorError.InvalidExpression)
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
        var value: BigDecimal = parseFactor()

        while (!isEnd()) {
            value = when (tokens[position]) {
                is Token.Operator.Multiply -> {
                    position++
                    value.multiply(parseFactor(), mathContext)
                }

                is Token.Operator.Divide -> {
                    position++
                    val divisor: BigDecimal = parseFactor()
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

    private fun parseFactor(): BigDecimal {
        if (isEnd()) {
            throw IllegalArgumentException("Expected factor")
        }

        return when (val token: Token = tokens[position]) {
            is Token.Sign.Positive -> {
                position++
                parseFactor()
            }

            is Token.Sign.Negative -> {
                position++
                parseFactor().negate(mathContext)
            }

            is Token.Numeric -> {
                position++
                token.value
            }

            is Token.FactorStart -> {
                position++
                val value: BigDecimal = parseExpression()
                if (isEnd() || tokens[position] !is Token.FactorEnd) {
                    throw IllegalArgumentException("Missing FactorEnd")
                }

                position++
                value
            }

            else -> {
                throw IllegalArgumentException("Expected factor")
            }
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
                    else -> return Result.Err(CalculatorError.Unsupposed)
                }
            )
        }
    }

    return Result.Ok(symbols)
}

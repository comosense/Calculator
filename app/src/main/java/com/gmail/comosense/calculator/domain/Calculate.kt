package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalculateError {
    InvalidExpression,
    DivisionByZero,
}

fun calculate(expression: List<Token>, precision: Int): Result<BigDecimal, CalculateError> {
    if (expression.isEmpty()) {
        return Result.Err(CalculateError.InvalidExpression)
    }

    val parser = Parser(expression, precision)

    return try {
        val result = parser.parseExpression()
        if (parser.isEnd()) {
            Result.Ok(result)
        } else {
            Result.Err(CalculateError.InvalidExpression)
        }
    } catch (_: ArithmeticException) {
        Result.Err(CalculateError.DivisionByZero)
    } catch (_: IllegalArgumentException) {
        Result.Err(CalculateError.InvalidExpression)
    }
}

private class Parser(private val tokens: List<Token>, precision: Int) {
    private val mathContext: MathContext = MathContext(
        precision,
        RoundingMode.HALF_UP,
    )

    private var position: Int = 0

    fun isEnd(): Boolean = (position >= tokens.size)

    fun parseExpression(): BigDecimal {
        var value = parseTerm()

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
        var value = parseFactor()

        while (!isEnd()) {
            value = when (tokens[position]) {
                is Token.Operator.Multiply -> {
                    position++
                    value.multiply(parseFactor(), mathContext)
                }

                is Token.Operator.Divide -> {
                    position++
                    val divisor = parseFactor()
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw ArithmeticException("Division by zero")
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

        return when (val token = tokens[position]) {
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
                val value = parseExpression()
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
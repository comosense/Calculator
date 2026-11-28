package com.gmail.comosense.calculator.domain

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.math.BigDecimal
import java.math.MathContext
import kotlin.coroutines.CoroutineContext

class IllegalFactorException : IllegalArgumentException()
class InvalidNumericFormatException : IllegalArgumentException()
class MissingFactorException : IllegalArgumentException()

class Parser(
    private val expression: List<Symbol>,
    private val isRadians: Boolean,
) {
    companion object {
        private const val EXTRA_PRECISION_FOR_WORKING: Int = 2
    }

    private class Cursor(
        private val symbols: List<Symbol>,
    ) {
        private var position: Int = 0

        fun isEnd(): Boolean =
            position >= symbols.size

        fun peek(): Symbol? =
            symbols.getOrNull(position)

        fun consume() {
            if (!isEnd()) {
                position++
            }
        }
    }

    suspend fun parse(
        mc: MathContext,
    ): UnifiedValue {
        val cursor = Cursor(expression)

        val parsed: UnifiedValue = parseExpression(
            cursor = cursor,
            mc = mc,
        )

        if (!cursor.isEnd()) {
            throw IllegalFactorException()
        }

        return parsed
    }

    private suspend fun parseExpression(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseTerm(
            cursor = cursor,
            mc = mc,
        )

        while (!cursor.isEnd()) {
            coroutineContext.ensureActive()

            value = when (cursor.peek()) {
                is Symbol.Operator.Add -> {
                    cursor.consume()

                    value.add(
                        other = parseTerm(
                            cursor = cursor,
                            mc = mc,
                        ),
                    )
                }

                is Symbol.Operator.Subtract -> {
                    cursor.consume()

                    value.subtract(
                        other = parseTerm(
                            cursor = cursor,
                            mc = mc,
                        ),
                    )
                }

                else -> break
            }
        }

        return value
    }

    private suspend fun parseTerm(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseSign(
            cursor = cursor,
            mc = mc,
        )

        while (!cursor.isEnd()) {
            coroutineContext.ensureActive()

            value = when (cursor.peek()) {
                is Symbol.Operator.Multiply -> {
                    cursor.consume()

                    value.multiply(
                        other = parseSign(
                            cursor = cursor,
                            mc = mc,
                        ),
                        mc = mc,
                    )
                }

                is Symbol.Operator.Divide -> {
                    cursor.consume()

                    value.divide(
                        other = parseSign(
                            cursor = cursor,
                            mc = mc,
                        ),
                        mc = mc,
                    )
                }

                else -> break
            }
        }

        return value
    }

    private suspend fun parseSign(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var negative = false

        while (!cursor.isEnd()) {
            coroutineContext.ensureActive()

            when (cursor.peek()) {
                is Symbol.Sign.Positive ->
                    cursor.consume()

                is Symbol.Sign.Negative -> {
                    cursor.consume()

                    negative = !negative
                }

                else -> break
            }
        }

        val value: UnifiedValue = parseBinaryOperation(
            cursor = cursor,
            mc = mc,
        )

        return if (negative) {
            value.negate()
        } else {
            value
        }
    }

    private suspend fun parseBinaryOperation(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val base: UnifiedValue = parsePostfixOperation(
            cursor = cursor,
            mc = mc,
        )

        return when (cursor.peek()) {
            is Symbol.Operator.Power -> {
                cursor.consume()

                base.pow(
                    exponent = parseSign(
                        cursor = cursor,
                        mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                    ),
                    mc = mc,
                )
            }

            else -> base
        }
    }

    private suspend fun parsePostfixOperation(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseFactor(
            cursor = cursor,
            mc = mc,
        )

        while (!cursor.isEnd()) {
            coroutineContext.ensureActive()

            when (cursor.peek()) {
                is Symbol.PostfixOperator.Factorial -> {
                    cursor.consume()

                    value = value.factorial()
                }

                else -> break
            }
        }

        return value
    }

    private suspend fun parseFactor(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue =
        when (val symbol: Symbol? = cursor.peek()) {
            is Symbol.Numeric ->
                parseNumeric(
                    cursor = cursor,
                )

            is Symbol.Constant -> {
                cursor.consume()

                parseConstant(
                    constant = symbol,
                    mc = mc,
                )
            }

            is Symbol.Factor.Function -> {
                cursor.consume()

                applyFunction(
                    function = symbol,
                    value = parseSign(
                        cursor = cursor,
                        mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                    ),
                    mc = mc,
                )
            }

            is Symbol.FactorStart.OpeningParenthesis -> {
                cursor.consume()

                parseFactorBody(
                    cursor = cursor,
                    mc = mc,
                )
            }

            is Symbol.FactorStart.Function -> {
                cursor.consume()

                applyFunction(
                    function = symbol,
                    value = parseFactorBody(
                        cursor = cursor,
                        mc = mc.withExtraPrecision(EXTRA_PRECISION_FOR_WORKING),
                    ),
                    mc = mc,
                )
            }

            else -> throw MissingFactorException()
        }

    private suspend fun parseFactorBody(
        cursor: Cursor,
        mc: MathContext,
    ): UnifiedValue {
        val value: UnifiedValue = parseExpression(
            cursor = cursor,
            mc = mc,
        )

        if (cursor.peek() !is Symbol.FactorEnd) {
            throw IllegalFactorException()
        }
        cursor.consume()

        return value
    }

    private suspend fun parseNumeric(
        cursor: Cursor,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()
        val buffer = StringBuilder()

        while (!cursor.isEnd()) {
            coroutineContext.ensureActive()

            when (val symbol: Symbol? = cursor.peek()) {
                is Symbol.Numeric.Digit -> {
                    cursor.consume()

                    buffer.append(symbol.value)
                }

                is Symbol.Numeric.Point -> {
                    cursor.consume()

                    buffer.append('.')
                }

                else -> break
            }
        }

        val value: BigDecimal = buffer.toString().toBigDecimalOrNull()
            ?: throw InvalidNumericFormatException()

        return UnifiedValue.fromExactValue(
            exactValue = value,
        )
    }

    private suspend fun parseConstant(
        constant: Symbol.Constant,
        mc: MathContext,
    ): UnifiedValue =
        when (constant) {
            Symbol.Constant.Pi ->
                UnifiedValue.pi()

            Symbol.Constant.Euler ->
                UnifiedValue.e(
                    mc = mc,
                )
        }

    private suspend fun applyFunction(
        function: Symbol.Factor.Function,
        value: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue =
        when (function) {
            Symbol.Factor.Function.Sqrt ->
                value.sqrt(
                    mc = mc,
                )
        }

    private suspend fun applyFunction(
        function: Symbol.FactorStart.Function,
        value: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue =
        when (function) {
            Symbol.FactorStart.Function.Exp ->
                value.exp(
                    mc = mc,
                )

            Symbol.FactorStart.Function.Ln ->
                value.ln(
                    mc = mc,
                )

            Symbol.FactorStart.Function.Log ->
                value.log(
                    mc = mc,
                )

            Symbol.FactorStart.Function.Sin ->
                value.sin(
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Cos ->
                value.cos(
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Tan ->
                value.tan(
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arcsin ->
                value.arcsin(
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arccos ->
                value.arccos(
                    isRadians = isRadians,
                    mc = mc,
                )

            Symbol.FactorStart.Function.Arctan ->
                value.arctan(
                    isRadians = isRadians,
                    mc = mc,
                )
        }
}

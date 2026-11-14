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

    private var position: Int = 0

    suspend fun parse(
        mc: MathContext,
    ): UnifiedValue {
        val parsed: UnifiedValue = parseExpression(
            mc = mc,
        )

        if (!isEnd()) {
            throw IllegalFactorException()
        }

        return parsed
    }

    private fun isEnd(): Boolean =
        position >= expression.size

    private suspend fun parseExpression(
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseTerm(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (expression[position]) {
                is Symbol.Operator.Add -> {
                    position++
                    value.add(parseTerm(mc))
                }

                is Symbol.Operator.Subtract -> {
                    position++
                    value.subtract(parseTerm(mc))
                }

                else ->
                    break
            }
        }

        return value
    }

    private suspend fun parseTerm(
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseSign(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            value = when (expression[position]) {
                is Symbol.Operator.Multiply -> {
                    position++
                    value.multiply(parseSign(mc), mc)
                }

                is Symbol.Operator.Divide -> {
                    position++
                    value.divide(parseSign(mc), mc)
                }

                else ->
                    break
            }
        }
        return value
    }

    private suspend fun parseSign(
        mc: MathContext,
    ): UnifiedValue {
        return when (expression.getOrNull(position)) {
            is Symbol.Sign.Positive -> {
                position++
                parseSign(mc)
            }

            is Symbol.Sign.Negative -> {
                position++
                parseSign(mc).negate()
            }

            else ->
                parseBinaryOperation(mc)
        }
    }

    private suspend fun parseBinaryOperation(
        mc: MathContext,
    ): UnifiedValue {
        val base: UnifiedValue = parsePostfixOperation(mc)

        return when (expression.getOrNull(position)) {
            is Symbol.Operator.Power -> {
                position++
                base.pow(
                    exponent = parseSign(workingMc(mc)),
                    mc = mc,
                )
            }

            else ->
                base
        }
    }

    private suspend fun parsePostfixOperation(
        mc: MathContext,
    ): UnifiedValue {
        val coroutineContext: CoroutineContext = currentCoroutineContext()

        var value: UnifiedValue = parseFactor(mc)

        while (!isEnd()) {
            coroutineContext.ensureActive()

            when (expression[position]) {
                is Symbol.PostfixOperator.Factorial -> {
                    position++
                    value = value.factorial()
                }

                else ->
                    break
            }
        }

        return value
    }

    private suspend fun parseFactor(
        mc: MathContext,
    ): UnifiedValue {
        if (isEnd()) {
            throw MissingFactorException()
        }

        return when (val symbol: Symbol = expression[position]) {
            is Symbol.Numeric ->
                parseNumeric()

            is Symbol.Constant -> {
                position++
                parseConstant(
                    constant = symbol,
                    mc = mc,
                )
            }

            is Symbol.Factor.Function -> {
                position++
                applyFunction(
                    function = symbol,
                    value = parseSign(workingMc(mc)),
                    mc = mc,
                )
            }

            is Symbol.FactorStart.OpeningParenthesis -> {
                position++
                parseFactorBody(
                    mc = mc,
                )
            }

            is Symbol.FactorStart.Function -> {
                position++
                applyFunction(
                    function = symbol,
                    value = parseFactorBody(workingMc(mc)),
                    mc = mc,
                )
            }

            else ->
                throw MissingFactorException()
        }
    }

    private suspend fun parseFactorBody(
        mc: MathContext,
    ): UnifiedValue {
        val value: UnifiedValue = parseExpression(mc)

        if (isEnd() || expression[position] !is Symbol.FactorEnd) {
            throw IllegalFactorException()
        }
        position++

        return value
    }

    private suspend fun parseNumeric(): UnifiedValue {
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

        val value: BigDecimal = buffer.toString().toBigDecimalOrNull()
            ?: throw InvalidNumericFormatException()

        return UnifiedValue.from(
            value = value,
        )
    }

    private suspend fun parseConstant(
        constant: Symbol.Constant,
        mc: MathContext,
    ): UnifiedValue {
        return when (constant) {
            Symbol.Constant.Pi ->
                UnifiedValue.pi()

            Symbol.Constant.Euler ->
                UnifiedValue.e(
                    mc = mc,
                )
        }
    }

    private suspend fun applyFunction(
        function: Symbol.Factor.Function,
        value: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        return when (function) {
            Symbol.Factor.Function.Sqrt ->
                value.sqrt(
                    mc = mc,
                )
        }
    }

    private suspend fun applyFunction(
        function: Symbol.FactorStart.Function,
        value: UnifiedValue,
        mc: MathContext,
    ): UnifiedValue {
        return when (function) {
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

    private fun workingMc(
        mc: MathContext,
    ): MathContext = MathContext(
        mc.precision + EXTRA_PRECISION_FOR_WORKING,
        mc.roundingMode,
    )
}

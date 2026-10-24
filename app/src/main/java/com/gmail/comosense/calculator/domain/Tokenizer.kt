package com.gmail.comosense.calculator.domain

import java.math.BigDecimal

fun tokenize(symbols: List<Symbol>): List<Token>? {
    val tokens: List<Token> = buildList {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric(): Boolean {
            return numericBuffer.isEmpty() || try {
                add(Token.Numeric(BigDecimal(numericBuffer.toString())))
                numericBuffer.clear()
                true
            } catch (_: NumberFormatException) {
                false
            }
        }

        for (symbol in symbols) {
            when (symbol) {
                is Symbol.Numeric.Digit ->
                    numericBuffer.append(symbol.value)

                is Symbol.Numeric.Point ->
                    numericBuffer.append(".")

                else -> {
                    if (!flushNumeric()) {
                        return null
                    }
                    add(symbol.toTokenOrNull() ?: return null)
                }
            }
        }
        if (!flushNumeric()) {
            return null
        }
    }

    return tokens
}

fun Symbol.toTokenOrNull(): Token? = when (this) {
    is Symbol.Sign.Positive ->
        Token.Sign.Positive

    is Symbol.Sign.Negative ->
        Token.Sign.Negative

    is Symbol.Constant.Pi ->
        Token.Constant.Pi

    is Symbol.Constant.Euler ->
        Token.Constant.Euler

    is Symbol.Factor.Function.Sqrt ->
        Token.Factor.Function.Sqrt

    is Symbol.Factor.Function.Pow10 ->
        Token.Factor.Function.Pow10

    is Symbol.FactorStart.OpeningParenthesis ->
        Token.FactorStart.OpeningParenthesis

    is Symbol.FactorStart.Function.Ln ->
        Token.FactorStart.Function.Ln

    is Symbol.FactorStart.Function.Log ->
        Token.FactorStart.Function.Log

    is Symbol.FactorStart.Function.Sin ->
        Token.FactorStart.Function.Sin

    is Symbol.FactorStart.Function.Cos ->
        Token.FactorStart.Function.Cos

    is Symbol.FactorStart.Function.Tan ->
        Token.FactorStart.Function.Tan

    is Symbol.FactorStart.Function.Exp ->
        Token.FactorStart.Function.Exp

    is Symbol.FactorStart.Function.Arcsin ->
        Token.FactorStart.Function.Arcsin

    is Symbol.FactorStart.Function.Arccos ->
        Token.FactorStart.Function.Arccos

    is Symbol.FactorStart.Function.Arctan ->
        Token.FactorStart.Function.Arctan

    is Symbol.FactorEnd.ClosingParenthesis ->
        Token.FactorEnd.ClosingParenthesis

    is Symbol.Operator.Add ->
        Token.Operator.Add

    is Symbol.Operator.Subtract ->
        Token.Operator.Subtract

    is Symbol.Operator.Multiply ->
        Token.Operator.Multiply

    is Symbol.Operator.Divide ->
        Token.Operator.Divide

    is Symbol.Operator.Power ->
        Token.Operator.Power

    is Symbol.PostfixOperator.Factorial ->
        Token.PostfixOperator.Factorial

    else ->
        null
}

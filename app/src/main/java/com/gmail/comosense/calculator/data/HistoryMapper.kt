package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.domain.Calculation
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.data.proto.Calculation as ProtoCalculation
import com.gmail.comosense.calculator.data.proto.Symbol as ProtoSymbol

fun ProtoSymbol.toSymbolOrNull(): Symbol? {
    return when (valueCase) {
        ProtoSymbol.ValueCase.POSITIVE ->
            Symbol.Sign.Positive

        ProtoSymbol.ValueCase.NEGATIVE ->
            Symbol.Sign.Negative

        ProtoSymbol.ValueCase.DIGIT ->
            if (digit in 0..9) {
                Symbol.Numeric.Digit(digit)
            } else {
                null
            }

        ProtoSymbol.ValueCase.POINT ->
            Symbol.Numeric.Point

        ProtoSymbol.ValueCase.OPEN_PARENTHESIS ->
            Symbol.FactorStart.OpenParenthesis

        ProtoSymbol.ValueCase.CLOSE_PARENTHESIS ->
            Symbol.FactorEnd.CloseParenthesis

        ProtoSymbol.ValueCase.ADD ->
            Symbol.Operator.Add

        ProtoSymbol.ValueCase.SUBTRACT ->
            Symbol.Operator.Subtract

        ProtoSymbol.ValueCase.MULTIPLY ->
            Symbol.Operator.Multiply

        ProtoSymbol.ValueCase.DIVIDE ->
            Symbol.Operator.Divide

        ProtoSymbol.ValueCase.ERROR ->
            Symbol.Error(error)

        ProtoSymbol.ValueCase.VALUE_NOT_SET ->
            null
    }
}

fun Symbol.toProto(): ProtoSymbol {
    return when (this) {
        Symbol.Sign.Positive ->
            ProtoSymbol.newBuilder()
                .setPositive(true)
                .build()

        Symbol.Sign.Negative ->
            ProtoSymbol.newBuilder()
                .setNegative(true)
                .build()

        is Symbol.Numeric.Digit ->
            ProtoSymbol.newBuilder()
                .setDigit(value)
                .build()

        Symbol.Numeric.Point ->
            ProtoSymbol.newBuilder()
                .setPoint(true)
                .build()

        Symbol.FactorStart.OpenParenthesis ->
            ProtoSymbol.newBuilder()
                .setOpenParenthesis(true)
                .build()

        Symbol.FactorEnd.CloseParenthesis ->
            ProtoSymbol.newBuilder()
                .setCloseParenthesis(true)
                .build()

        Symbol.Operator.Add ->
            ProtoSymbol.newBuilder()
                .setAdd(true)
                .build()

        Symbol.Operator.Subtract ->
            ProtoSymbol.newBuilder()
                .setSubtract(true)
                .build()

        Symbol.Operator.Multiply ->
            ProtoSymbol.newBuilder()
                .setMultiply(true)
                .build()

        Symbol.Operator.Divide ->
            ProtoSymbol.newBuilder()
                .setDivide(true)
                .build()

        is Symbol.Error ->
            ProtoSymbol.newBuilder()
                .setError(value)
                .build()
    }
}

fun ProtoCalculation.toCalculationOrNull(): Calculation? {
    val expression = buildList {
        for (symbol in expressionList) {
            add(symbol.toSymbolOrNull() ?: return null)
        }
    }

    val result = buildList {
        for (symbol in resultList) {
            add(symbol.toSymbolOrNull() ?: return null)
        }
    }

    return Calculation(
        expression = expression,
        result = result,
    )
}

fun Calculation.toProto(): ProtoCalculation {
    return ProtoCalculation
        .newBuilder()
        .addAllExpression(expression.map { it.toProto() })
        .addAllResult(result.map { it.toProto() })
        .build()
}

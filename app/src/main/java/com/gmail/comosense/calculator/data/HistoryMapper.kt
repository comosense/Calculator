package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.presentation.Calculation
import com.gmail.comosense.calculator.presentation.Symbol
import com.gmail.comosense.calculator.data.proto.Calculation as ProtoCalculation
import com.gmail.comosense.calculator.data.proto.Symbol as ProtoSymbol

fun ProtoSymbol.toSymbol(): Symbol {
    return when (valueCase) {
        ProtoSymbol.ValueCase.POSITIVE
            -> Symbol.Sign.Positive

        ProtoSymbol.ValueCase.NEGATIVE
            -> Symbol.Sign.Negative

        ProtoSymbol.ValueCase.DIGIT
            -> Symbol.Numeric.Digit(digit)

        ProtoSymbol.ValueCase.POINT
            -> Symbol.Numeric.Point

        ProtoSymbol.ValueCase.OPEN_PARENTHESIS
            -> Symbol.FactorStart.OpenParenthesis

        ProtoSymbol.ValueCase.CLOSE_PARENTHESIS
            -> Symbol.FactorEnd.CloseParenthesis

        ProtoSymbol.ValueCase.ADD
            -> Symbol.Operator.Add

        ProtoSymbol.ValueCase.SUBTRACT
            -> Symbol.Operator.Subtract

        ProtoSymbol.ValueCase.MULTIPLY
            -> Symbol.Operator.Multiply

        ProtoSymbol.ValueCase.DIVIDE
            -> Symbol.Operator.Divide

        ProtoSymbol.ValueCase.ERROR
            -> Symbol.Error(error)

        ProtoSymbol.ValueCase.VALUE_NOT_SET
            -> error("Invalid stored Symbol")
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

fun ProtoCalculation.toCalculation(): Calculation {
    return Calculation(
        expression = expressionList.map { it.toSymbol() },
        result = resultList.map { it.toSymbol() },
    )
}

fun Calculation.toProto(): ProtoCalculation {
    return ProtoCalculation
        .newBuilder()
        .addAllExpression(expression.map { it.toProto() })
        .addAllResult(result.map { it.toProto() })
        .build()
}

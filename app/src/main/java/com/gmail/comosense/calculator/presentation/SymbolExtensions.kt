package com.gmail.comosense.calculator.presentation

val Symbol.text: String
    get() = when (this) {
        is Symbol.Sign.Positive
            -> "+"

        is Symbol.Sign.Negative
            -> "-"

        is Symbol.Numeric.Digit
            -> value.toString()

        is Symbol.Numeric.Point
            -> "."

        is Symbol.FactorStart.OpenParenthesis
            -> "("

        is Symbol.FactorEnd.CloseParenthesis
            -> ")"

        is Symbol.Operator.Add
            -> "+"

        is Symbol.Operator.Subtract
            -> "-"

        is Symbol.Operator.Multiply
            -> "×"

        is Symbol.Operator.Divide
            -> "÷"

        is Symbol.Error
            -> value
    }

fun Symbol.isAppendableAfter(previous: Symbol?): Boolean = when (this) {
    is Symbol.Numeric.Digit ->
        previous !is Symbol.FactorEnd

    is Symbol.Numeric.Point ->
        previous is Symbol.Numeric.Digit

    is Symbol.Sign ->
        previous == null ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.FactorStart ->
        previous == null ||
                previous is Symbol.Sign ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.FactorEnd ->
        previous is Symbol.Numeric.Digit ||
                previous is Symbol.FactorEnd

    is Symbol.Operator ->
        previous is Symbol.Numeric.Digit ||
                previous is Symbol.FactorEnd

    is Symbol.Error -> false
}

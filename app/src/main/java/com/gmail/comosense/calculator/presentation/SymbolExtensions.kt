package com.gmail.comosense.calculator.presentation

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

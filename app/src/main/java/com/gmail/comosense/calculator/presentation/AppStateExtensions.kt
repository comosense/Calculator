package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

val AppState.expression: List<Symbol>
    get() = result + entering
val AppState.isEntering: Boolean
    get() = entering.isNotEmpty()

fun AppState.canAppend(symbol: Symbol): Boolean = this.canAppend(listOf(symbol))
fun AppState.canAppend(symbols: List<Symbol>): Boolean {
    val tempEntering: MutableList<Symbol> = entering.toMutableList()

    for (symbol in symbols) {
        if (when (symbol) {
                is Symbol.Numeric.Point ->
                    symbol.isAppendableAfter(tempEntering.lastOrNull()) &&
                            tempEntering
                                .takeLastWhile { it is Symbol.Numeric }
                                .none { it is Symbol.Numeric.Point }

                is Symbol.FactorEnd.ClosingParenthesis ->
                    symbol.isAppendableAfter(tempEntering.lastOrNull()) &&
                            (tempEntering.count { it is Symbol.FactorStart }
                                    > tempEntering.count { it is Symbol.FactorEnd })

                is Symbol.Operator ->
                    symbol.isAppendableAfter((result + tempEntering).lastOrNull())

                else ->
                    symbol.isAppendableAfter(tempEntering.lastOrNull())
            }
        ) {
            tempEntering.add(symbol)
        } else {
            return false
        }
    }
    return true
}

private fun Symbol.isAppendableAfter(previous: Symbol?): Boolean = when (this) {
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
}

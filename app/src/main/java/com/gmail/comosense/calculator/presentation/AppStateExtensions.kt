package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

val AppState.expression: List<Symbol>
    get() = result + entering
val AppState.isEntering: Boolean
    get() = entering.isNotEmpty()

fun AppState.displayExpression(symbolFormatter: SymbolFormatter): String {
    return symbolFormatter.format(expression).ifEmpty { "0" }
}

fun AppState.canAppend(symbol: Symbol): Boolean = this.canAppend(listOf(symbol))

fun AppState.canAppend(symbols: List<Symbol>): Boolean {
    val tempExpression: MutableList<Symbol> = expression.toMutableList()

    for (symbol in symbols) {
        if (when (symbol) {
                is Symbol.Numeric.Point -> {
                    tempExpression.isNotEmpty() &&
                            symbol.isAppendableAfter(tempExpression.lastOrNull()) &&
                            tempExpression
                                .takeLastWhile {
                                    it is Symbol.Numeric.Digit || it is Symbol.Numeric.Point
                                }
                                .none { it is Symbol.Numeric.Point }
                }

                is Symbol.FactorEnd -> {
                    tempExpression.isNotEmpty() &&
                            symbol.isAppendableAfter(tempExpression.lastOrNull()) &&
                            (tempExpression.count { it is Symbol.FactorStart }
                                    > tempExpression.count { it is Symbol.FactorEnd })
                }

                else -> {
                    symbol.isAppendableAfter(tempExpression.lastOrNull())
                }
            }
        ) {
            tempExpression.add(symbol)
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

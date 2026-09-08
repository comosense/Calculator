package com.gmail.comosense.calculator.presentation

import java.util.Locale

val AppState.expression: List<Symbol>
    get() = result + entering
val AppState.isEntering: Boolean
    get() = entering.isNotEmpty()
val AppState.lastSymbol: Symbol?
    get() = expression.lastOrNull()

fun AppState.displayExpression(locale: Locale): String {
    return expression.formatSymbols(locale).ifEmpty { "0" }
}

fun AppState.canAppend(symbol: Symbol): Boolean = when (symbol) {
    is Symbol.Numeric.Point -> {
        entering.isNotEmpty() &&
                symbol.isAppendableAfter(lastSymbol) &&
                entering
                    .takeLastWhile { it is Symbol.Numeric.Digit || it is Symbol.Numeric.Point }
                    .none { it is Symbol.Numeric.Point }
    }

    is Symbol.FactorEnd -> {
        entering.isNotEmpty() &&
                symbol.isAppendableAfter(lastSymbol) &&
                (entering.count { it is Symbol.FactorStart } > entering.count { it is Symbol.FactorEnd })
    }

    else -> {
        symbol.isAppendableAfter(lastSymbol)
    }
}

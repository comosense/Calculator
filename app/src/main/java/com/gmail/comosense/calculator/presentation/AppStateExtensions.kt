package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.domain.isAppendableAfter
import java.util.Locale

val AppState.expression: List<Symbol>
    get() = result + entering
val AppState.isEntering: Boolean
    get() = entering.isNotEmpty()

fun AppState.displayExpression(locale: Locale): String {
    return expression.formatSymbols(locale).ifEmpty { "0" }
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

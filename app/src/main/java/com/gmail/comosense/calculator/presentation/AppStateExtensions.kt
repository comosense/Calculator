package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.formatNumericString

val AppState.expression: List<Symbol>
    get() = result + entering
val AppState.displayExpression: String
    get() = expression.formatSymbols().ifEmpty { "0" }
val AppState.isEntering: Boolean
    get() = entering.isNotEmpty()
val AppState.lastSymbol: Symbol?
    get() = expression.lastOrNull()

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

private fun List<Symbol>.formatSymbols(): String {
    return buildString {
        val numericBuffer: StringBuilder = StringBuilder()

        fun flushNumeric() {
            if (numericBuffer.isNotEmpty()) {
                append(formatNumericString(numericBuffer.toString()))
                numericBuffer.clear()
            }
        }

        for (symbol in this@formatSymbols) {
            when (symbol) {
                is Symbol.Numeric.Digit, Symbol.Numeric.Point -> numericBuffer.append(symbol.text)
                else -> {
                    flushNumeric()
                    append(symbol.text)
                }
            }
        }

        flushNumeric()
    }
}

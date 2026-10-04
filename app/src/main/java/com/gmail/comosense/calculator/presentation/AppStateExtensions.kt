package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

val AppState.expression: List<Symbol>
    get() = result + input
val AppState.isInputting: Boolean
    get() = input.isNotEmpty()

fun AppState.appendExpression(symbol: Symbol): AppState {
    val symbols: List<Symbol> = symbolsToAppendOrNull(symbol) ?: return this

    return if (symbol is Symbol.Operator || isInputting) {
        copy(
            input = input + symbols,
        )
    } else {
        copy(
            result = emptyList(),
            input = input + symbols,
        )
    }
}

fun AppState.applyResult(result: List<Symbol>): AppState {
    return copy(
        result = result,
        input = emptyList(),
    )
}

fun AppState.applyHistory(id: String): AppState {
    val result = (histories.firstOrNull { it.id == id } ?: return this).result

    return if (isInputting) {
        if (canAppend(result)) {
            copy(
                input = input + result,
            )
        } else {
            this
        }
    } else {
        applyResult(result)
    }
}

fun AppState.clearExpression(): AppState {
    return copy(
        result = emptyList(),
        input = emptyList(),
    )
}

fun AppState.dropLastExpressionSymbol(): AppState {
    return if (isInputting) {
        copy(
            input = input.dropLast(1),
        )
    } else {
        this
    }
}

fun AppState.canAppend(symbol: Symbol): Boolean = this.canAppend(listOf(symbol))
fun AppState.canAppend(symbols: List<Symbol>): Boolean {
    val tempInput: MutableList<Symbol> = input.toMutableList()

    for (symbol in symbols) {
        if (when (symbol) {
                is Symbol.Numeric.Point ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            tempInput
                                .takeLastWhile { it is Symbol.Numeric }
                                .none { it is Symbol.Numeric.Point }

                is Symbol.FactorEnd.ClosingParenthesis ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            (tempInput.count { it is Symbol.FactorStart }
                                    > tempInput.count { it is Symbol.FactorEnd })

                is Symbol.Operator ->
                    symbol.isAppendableAfter((result + tempInput).lastOrNull())

                else ->
                    symbol.isAppendableAfter(tempInput.lastOrNull())
            }
        ) {
            tempInput.add(symbol)
        } else {
            return false
        }
    }
    return true
}

private fun AppState.symbolsToAppendOrNull(symbol: Symbol): List<Symbol>? {
    if (canAppend(symbol)) return listOf(symbol)

    return when (symbol) {
        is Symbol.Numeric.Point ->
            listOf(Symbol.Numeric.Digit(0), symbol)

        is Symbol.Constant,
        is Symbol.Factor,
        is Symbol.FactorStart ->
            listOf(Symbol.Operator.Multiply, symbol)

        else ->
            return null
    }.takeIf(::canAppend)
}

private fun Symbol.isAppendableAfter(previous: Symbol?): Boolean = when (this) {
    is Symbol.Sign ->
        previous == null ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.Numeric.Digit ->
        previous !is Symbol.Constant &&
                previous !is Symbol.FactorEnd

    is Symbol.Numeric.Point ->
        previous is Symbol.Numeric.Digit

    is Symbol.Constant,
    is Symbol.Factor,
    is Symbol.FactorStart ->
        previous == null ||
                previous is Symbol.Sign ||
                previous is Symbol.Factor.Function ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.FactorEnd,
    is Symbol.Operator ->
        previous is Symbol.Numeric.Digit ||
                previous is Symbol.Constant ||
                previous is Symbol.FactorEnd
}

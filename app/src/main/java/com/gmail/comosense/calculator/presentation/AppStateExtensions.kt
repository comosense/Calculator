package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.domain.Symbol

internal object AppStateExtensionsConstrains {
    const val MAX_EXPRESSION_SIZE: Int = Constraints.MAX_EXPRESSION_SIZE
}

sealed interface AppStateUpdateResult {
    data class Success(val state: AppState) : AppStateUpdateResult
    data object ExpressionTooLarge : AppStateUpdateResult
    data object UnacceptableSymbol : AppStateUpdateResult
}

val AppState.expression: List<Symbol>
    get() = result + input
val AppState.isInputting: Boolean
    get() = input.isNotEmpty()

fun AppState.appendExpression(symbol: Symbol): AppStateUpdateResult {
    val symbols: List<Symbol> = symbolsToAppendOrNull(symbol)
        ?: return AppStateUpdateResult.UnacceptableSymbol

    if (expression.size + symbols.size > AppStateExtensionsConstrains.MAX_EXPRESSION_SIZE) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        if (symbol is Symbol.Operator || isInputting) {
            copy(
                input = input + symbols,
            )
        } else {
            copy(
                result = emptyList(),
                input = input + symbols,
            )
        }
    )
}

fun AppState.applyResult(result: List<Symbol>): AppStateUpdateResult {
    if (result.size > AppStateExtensionsConstrains.MAX_EXPRESSION_SIZE) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        copy(
            result = result,
            input = emptyList(),
        )
    )
}

fun AppState.applyHistory(id: String): AppStateUpdateResult {
    val result: List<Symbol> = histories
        .firstOrNull { it.id == id }
        ?.result
        ?: return AppStateUpdateResult.UnacceptableSymbol

    if (!isInputting) {
        return applyResult(result)
    }

    if (!canAppend(result)) {
        return AppStateUpdateResult.UnacceptableSymbol
    }

    if (expression.size + result.size > AppStateExtensionsConstrains.MAX_EXPRESSION_SIZE) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        copy(
            input = input + result,
        )
    )
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
                previous is Symbol.Factor ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator ||
                previous is Symbol.SpecialOperator.Power

    is Symbol.Numeric.Digit ->
        previous !is Symbol.Constant &&
                previous !is Symbol.FactorEnd &&
                previous !is Symbol.SpecialOperator.Factorial

    is Symbol.Numeric.Point ->
        previous is Symbol.Numeric.Digit

    is Symbol.Constant,
    is Symbol.Factor,
    is Symbol.FactorStart ->
        previous == null ||
                previous is Symbol.Sign ||
                previous is Symbol.Factor ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator ||
                previous is Symbol.SpecialOperator.Power

    is Symbol.FactorEnd,
    is Symbol.Operator,
    is Symbol.SpecialOperator ->
        previous is Symbol.Numeric.Digit ||
                previous is Symbol.Constant ||
                previous is Symbol.FactorEnd ||
                previous is Symbol.SpecialOperator.Factorial
}

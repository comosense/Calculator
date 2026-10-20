package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.domain.Symbol

internal object AppStateExtensionsConstraints {
    const val MAX_EXPRESSION_SIZE: Int = Constraints.MAX_EXPRESSION_SIZE
    const val MAX_NESTING_DEPTH: Int = Constraints.MAX_NESTING_DEPTH
}

sealed interface AppStateUpdateResult {
    data class Success(val appState: AppState) : AppStateUpdateResult
    data object ExpressionTooLarge : AppStateUpdateResult
    data object UnacceptableSymbol : AppStateUpdateResult
    data object HistoryNotFound : AppStateUpdateResult
}

val AppState.fixedExpression: List<Symbol>
    get() = result + input
val AppState.completionInput: List<Symbol>
    get() = List(
        (input.count { symbol -> symbol is Symbol.FactorStart } -
                input.count { symbol -> symbol is Symbol.FactorEnd })
            .coerceAtLeast(0)
    ) { Symbol.FactorEnd.ClosingParenthesis }
val AppState.expression: List<Symbol>
    get() = fixedExpression + completionInput
val AppState.isInputting: Boolean
    get() = input.isNotEmpty()

fun AppState.appendExpression(symbol: Symbol): AppStateUpdateResult {
    val symbols: List<Symbol> = symbolsToAppendOrNull(symbol)
        ?: return AppStateUpdateResult.UnacceptableSymbol

    if (expression.size + symbols.size
        > AppStateExtensionsConstraints.MAX_EXPRESSION_SIZE
    ) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        appState = if (
            symbol is Symbol.Operator ||
            symbol is Symbol.SpecialOperator ||
            isInputting
        ) {
            copy(
                input = input + symbols,
            )
        } else {
            copy(
                result = emptyList(),
                input = input + symbols,
            )
        },
    )
}

fun AppState.applyResult(result: List<Symbol>): AppStateUpdateResult {
    if (result.size > AppStateExtensionsConstraints.MAX_EXPRESSION_SIZE) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        appState = copy(
            result = result,
            input = emptyList(),
        ),
    )
}

fun AppState.applyHistory(id: String): AppStateUpdateResult {
    val result: List<Symbol> = histories
        .firstOrNull { history -> history.id == id }
        ?.result
        ?: return AppStateUpdateResult.HistoryNotFound

    if (!isInputting) {
        return applyResult(result)
    }

    if (!canAppend(result)) {
        return AppStateUpdateResult.UnacceptableSymbol
    }

    if (expression.size + result.size > AppStateExtensionsConstraints.MAX_EXPRESSION_SIZE) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        appState = copy(
            input = input + result,
        ),
    )
}

fun AppState.clearExpression(): AppStateUpdateResult {
    return AppStateUpdateResult.Success(
        appState = copy(
            result = emptyList(),
            input = emptyList(),
        ),
    )
}

fun AppState.dropLastExpressionSymbol(): AppStateUpdateResult {
    return AppStateUpdateResult.Success(
        appState = if (isInputting) {
            copy(
                input = input.dropLast(1),
            )
        } else {
            this
        },
    )
}

fun AppState.angle(isRadians: Boolean): AppStateUpdateResult {
    return AppStateUpdateResult.Success(
        appState = copy(
            isRadians = isRadians,
        ),
    )
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

fun AppState.canAppend(symbol: Symbol): Boolean = this.canAppend(listOf(symbol))
fun AppState.canAppend(symbols: List<Symbol>): Boolean {
    val tempInput: MutableList<Symbol> = input.toMutableList()
    val nestingDepth: Int =
        tempInput.count { symbol -> symbol is Symbol.FactorStart } -
                tempInput.count { symbol -> symbol is Symbol.FactorEnd }

    for (symbol in symbols) {
        if (when (symbol) {
                is Symbol.Numeric.Point ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            tempInput
                                .takeLastWhile { symbol -> symbol is Symbol.Numeric }
                                .none { symbol -> symbol is Symbol.Numeric.Point }

                is Symbol.FactorStart ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            nestingDepth < AppStateExtensionsConstraints.MAX_NESTING_DEPTH

                is Symbol.FactorEnd ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            nestingDepth > 0

                is Symbol.Operator,
                is Symbol.SpecialOperator ->
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

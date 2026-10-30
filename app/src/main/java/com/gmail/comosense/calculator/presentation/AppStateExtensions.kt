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
    get() = List(input.nestingDepth().coerceAtLeast(0)) {
        Symbol.FactorEnd.ClosingParenthesis
    }
val AppState.expression: List<Symbol>
    get() = fixedExpression + completionInput
val AppState.isInputting: Boolean
    get() = input.isNotEmpty()

fun AppState.appendExpression(
    symbols: List<Symbol>,
): AppStateUpdateResult {
    val symbolsToAppend: List<Symbol> = symbolsToAppendOrNull(symbols)
        ?: return AppStateUpdateResult.UnacceptableSymbol
    val firstSymbol: Symbol = symbolsToAppend.firstOrNull()
        ?: return AppStateUpdateResult.UnacceptableSymbol

    if (expression.size + symbolsToAppend.size
        > AppStateExtensionsConstraints.MAX_EXPRESSION_SIZE
    ) {
        return AppStateUpdateResult.ExpressionTooLarge
    }

    return AppStateUpdateResult.Success(
        appState = if (
            firstSymbol is Symbol.Operator ||
            firstSymbol is Symbol.PostfixOperator ||
            isInputting
        ) {
            copy(
                input = input + symbolsToAppend,
            )
        } else {
            copy(
                result = emptyList(),
                input = input + symbolsToAppend,
            )
        },
    )
}

fun AppState.applyResult(
    result: List<Symbol>,
): AppStateUpdateResult {
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

fun AppState.applyHistory(
    id: String,
): AppStateUpdateResult {
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

fun AppState.toggleAngleMode(): AppStateUpdateResult {
    return AppStateUpdateResult.Success(
        appState = copy(
            isRadians = !isRadians,
        ),
    )
}

fun AppState.canAppend(
    symbol: Symbol,
): Boolean = canAppend(listOf(symbol))

fun AppState.canAppend(
    symbols: List<Symbol>,
): Boolean {
    val tempInput: MutableList<Symbol> = input.toMutableList()

    var nestingDepth: Int = input.nestingDepth()

    for (symbol in symbols) {
        if (when (symbol) {
                is Symbol.Numeric.Point ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            tempInput
                                .takeLastWhile { joiningSymbol -> joiningSymbol is Symbol.Numeric }
                                .none { joiningSymbol -> joiningSymbol is Symbol.Numeric.Point }

                is Symbol.FactorStart ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            nestingDepth < AppStateExtensionsConstraints.MAX_NESTING_DEPTH

                is Symbol.FactorEnd ->
                    symbol.isAppendableAfter(tempInput.lastOrNull()) &&
                            nestingDepth > 0

                is Symbol.Operator,
                is Symbol.PostfixOperator ->
                    symbol.isAppendableAfter(tempInput.lastOrNull() ?: result.lastOrNull())

                else ->
                    symbol.isAppendableAfter(tempInput.lastOrNull())
            }
        ) {
            tempInput.add(symbol)

            when (symbol) {
                is Symbol.FactorStart -> nestingDepth++
                is Symbol.FactorEnd -> nestingDepth--
                else -> Unit
            }
        } else {
            return false
        }
    }
    return true
}

private fun AppState.symbolsToAppendOrNull(
    symbols: List<Symbol>,
): List<Symbol>? {
    return if (canAppend(symbols)) {
        symbols
    } else {
        when (symbols.firstOrNull()) {
            is Symbol.Numeric.Point ->
                listOf(Symbol.Numeric.Digit(0)) + symbols

            is Symbol.Numeric.Digit,
            is Symbol.Constant,
            is Symbol.Factor,
            is Symbol.FactorStart ->
                listOf(Symbol.Operator.Multiply) + symbols

            else ->
                return null
        }.takeIf(::canAppend)
    }
}

private fun Symbol.isAppendableAfter(
    previous: Symbol?,
): Boolean = when (this) {
    is Symbol.Sign ->
        previous == null ||
                previous is Symbol.Factor ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.Numeric.Digit ->
        previous !is Symbol.Constant &&
                previous !is Symbol.FactorEnd &&
                previous !is Symbol.PostfixOperator

    is Symbol.Numeric.Point ->
        previous is Symbol.Numeric.Digit

    is Symbol.Constant,
    is Symbol.Factor,
    is Symbol.FactorStart ->
        previous == null ||
                previous is Symbol.Sign ||
                previous is Symbol.Factor ||
                previous is Symbol.FactorStart ||
                previous is Symbol.Operator

    is Symbol.FactorEnd,
    is Symbol.Operator,
    is Symbol.PostfixOperator ->
        previous is Symbol.Numeric.Digit ||
                previous is Symbol.Constant ||
                previous is Symbol.FactorEnd ||
                previous is Symbol.PostfixOperator
}

private fun Iterable<Symbol>.nestingDepth(): Int = sumOf { symbol ->
    when (symbol) {
        is Symbol.FactorStart -> 1
        is Symbol.FactorEnd -> -1
        else -> 0
    }
}

package com.gmail.comosense.calculator.presentation

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.domain.CalculateError
import com.gmail.comosense.calculator.domain.calculate

class AppViewModel : ViewModel() {
    companion object {
        private const val DISPLAY_SCALE = 20
        private const val PRECISION = 50
        private const val HISTORY_SIZE = 50
    }

    private val _appState: MutableState<AppState> = mutableStateOf(AppState())
    val appState: State<AppState> = _appState

    fun onClick(key: Key) {
        when (key) {
            is CommandKey -> updateState(key)
            is SymbolKey -> updateState(key)
        }
    }

    fun onLongClick(key: Key?) {
        if (key != null) {
            onClick(key)
        }
    }

    fun onHistoryClick(historyResult: List<Symbol>) {
        val state: AppState = _appState.value
        if (state.isEntering) {
            if (state.lastSymbol !is Symbol.Numeric) {
                _appState.value = state.copy(
                    entering = state.entering + historyResult
                )
            }
        } else {
            _appState.value = state.copy(
                result = historyResult
            )
        }
    }

    fun onHistoryDelete(index: Int) {
        val state: AppState = _appState.value
        if (index !in state.history.indices) return
        _appState.value = state.copy(
            history = state.history.filterIndexed { i, _ -> i != index }
        )
    }

    fun onHistoryDeleteAll() {
        _appState.value = _appState.value.copy(
            history = emptyList()
        )
    }

    private fun updateState(key: CommandKey) {
        val state: AppState = _appState.value

        when (key) {
            is CommandKey.Equals -> {
                if (!state.isEntering) return

                val result: List<Symbol> = calculateSymbols(state.expression)
                val history: List<Calculation> = (listOf(
                    Calculation(
                        expression = state.expression,
                        result = result
                    )
                ) + state.history).take(HISTORY_SIZE)

                _appState.value = state.copy(
                    result = result,
                    entering = emptyList(),
                    history = history,
                )
            }

            is CommandKey.Clear -> {
                _appState.value = state.copy(
                    result = emptyList(),
                    entering = emptyList(),
                )
            }

            is CommandKey.Delete -> {
                _appState.value = state.copy(
                    entering = state.entering.dropLast(1),
                )
            }
        }
    }

    private fun updateState(key: SymbolKey) {
        val state: AppState = _appState.value
        val symbol: Symbol = key.symbol

        if (!state.canAppend(symbol)) return

        when (symbol) {
            is Symbol.Operator -> {
                _appState.value = state.copy(
                    entering = state.entering + symbol,
                )
            }

            else -> {
                _appState.value = state.copy(
                    result = if (state.entering.isEmpty()) emptyList() else state.result,
                    entering = state.entering + symbol,
                )
            }
        }
    }

    private fun calculateSymbols(expression: List<Symbol>): List<Symbol> {
        return when (val r = tokensFrom(expression)) {
            is Result.Ok -> {
                when (val r = calculate(r.value, PRECISION).toSymbols(DISPLAY_SCALE)) {
                    is Result.Ok -> r.value
                    is Result.Err -> listOf(Symbol.Error(errorMessage(r.error)))
                }
            }

            is Result.Err -> {
                Log.e("AppViewModel", "[calculate]${r.error}")
                listOf(Symbol.Error(errorMessage(r.error)))
            }
        }
    }

    private fun errorMessage(error: SymbolConverterError): String = when (error) {
        is SymbolConverterError.UnsupportedSymbol,
        is SymbolConverterError.IllegalNumeric
            -> "Invalid expression"

        is SymbolConverterError.UnsupportedCharacter
            -> "Invalid result"

        is SymbolConverterError.Calculation -> when (error.error) {
            CalculateError.InvalidExpression
                -> "Invalid expression"

            CalculateError.DivisionByZero
                -> "Division by zero"
        }
    }
}

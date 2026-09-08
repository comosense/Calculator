package com.gmail.comosense.calculator.presentation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class AppViewModel : ViewModel() {
    companion object {
        private const val PRECISION = 50
        private const val DISPLAY_SCALE = 20
        private const val HISTORY_SIZE = 50
    }

    private val calculatorService = CalculatorService(
        precision = PRECISION,
        displayScale = DISPLAY_SCALE
    )
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

                val result: List<Symbol> = calculatorService.calculate(state.expression)

                val calculation = Calculation(state.expression, result)

                _appState.value = state.copy(
                    result = result,
                    entering = emptyList(),
                    history = (listOf(calculation) + state.history).take(HISTORY_SIZE),
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
}

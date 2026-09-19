package com.gmail.comosense.calculator.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gmail.comosense.calculator.data.HistoryRepository
import com.gmail.comosense.calculator.data.historyDataStore
import com.gmail.comosense.calculator.domain.Symbol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppAction {
    data class Input(val symbol: Symbol) : AppAction
    data object Calculate : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
    data class SelectHistory(val result: List<Symbol>) : AppAction
    data class DeleteHistory(val index: Int) : AppAction
    data object DeleteHistoryAll : AppAction
}

class AppViewModel(private val historyRepository: HistoryRepository) : ViewModel() {
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(
                    historyRepository = HistoryRepository(context.historyDataStore),
                ) as T
            }
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }

    companion object {
        private const val PRECISION: Int = 50
        private const val DISPLAY_SCALE: Int = 20
    }

    private val calculatorService = CalculatorService(
        precision = PRECISION,
        displayScale = DISPLAY_SCALE,
    )
    private val _appState: MutableStateFlow<AppState> = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.history.collect { history ->
                _appState.value = _appState.value.copy(
                    history = history,
                )
            }
        }
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.Input -> appendSymbol(action.symbol)
            is AppAction.Calculate -> calculate()
            is AppAction.Clear -> clear()
            is AppAction.Backspace -> backspace()
            is AppAction.SelectHistory -> selectHistory(action.result)
            is AppAction.DeleteHistory -> deleteHistory(action.index)
            is AppAction.DeleteHistoryAll -> deleteHistoryAll()
        }
    }

    private fun appendSymbol(symbol: Symbol) {
        _appState.update { state ->
            if (!state.canAppend(symbol)) return@update state

            when (symbol) {
                is Symbol.Operator -> {
                    state.copy(
                        entering = state.entering + symbol,
                    )
                }

                else -> {
                    state.copy(
                        result = if (state.entering.isEmpty()) {
                            emptyList()
                        } else {
                            state.result
                        },
                        entering = state.entering + symbol,
                    )
                }
            }
        }
    }

    private fun calculate() {
        val state: AppState = _appState.value

        if (!state.isEntering) return

        val result: List<Symbol> = calculatorService.calculate(state.expression)

        _appState.update {
            it.copy(
                result = result,
                entering = emptyList(),
            )
        }

        viewModelScope.launch {
            historyRepository.addHistory(
                Calculation(
                    expression = state.expression,
                    result = result,
                )
            )
        }
    }

    private fun clear() {
        _appState.update { state ->
            state.copy(
                result = emptyList(),
                entering = emptyList(),
            )
        }
    }

    private fun backspace() {
        _appState.update { state ->
            state.copy(
                entering = state.entering.dropLast(1),
            )
        }
    }

    private fun selectHistory(result: List<Symbol>) {
        _appState.update { state ->
            if (state.isEntering) {
                if (state.lastSymbol !is Symbol.Numeric) {
                    state.copy(
                        entering = state.entering + result,
                    )
                } else {
                    state
                }
            } else {
                state.copy(
                    result = result,
                )
            }
        }
    }

    fun deleteHistory(index: Int) {
        viewModelScope.launch {
            historyRepository.deleteHistory(index)
        }
    }

    fun deleteHistoryAll() {
        viewModelScope.launch {
            historyRepository.deleteAllHistory()
        }
    }
}

package com.gmail.comosense.calculator.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.data.HistoryRepository
import com.gmail.comosense.calculator.data.historyDataStore
import com.gmail.comosense.calculator.domain.CalculatorError
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.domain.calculate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppAction {
    data class Input(val symbol: Symbol) : AppAction
    data object Calculate : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
    data class SelectHistory(val result: List<Symbol>) : AppAction
    data class DeleteHistory(val id: String) : AppAction
    data object DeleteHistoryAll : AppAction
}

class AppViewModel(private val historyRepository: HistoryRepository) : ViewModel() {
    companion object {
        private const val CALCULATION_PRECISION: Int = 50
        private const val DISPLAY_SCALE: Int = 20
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(HistoryRepository(application.historyDataStore)) as T
            }
            throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }

    private val _appState: MutableStateFlow<AppState> =
        MutableStateFlow(AppState())
    val appState: StateFlow<AppState> =
        _appState.asStateFlow()

    private val _errorEvent: MutableSharedFlow<CalculatorError> =
        MutableSharedFlow(extraBufferCapacity = 1)
    val errorEvent: SharedFlow<CalculatorError> =
        _errorEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            historyRepository.history.collect { histories ->
                _appState.update { state ->
                    state.copy(histories = histories)
                }
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
            is AppAction.DeleteHistory -> deleteHistory(action.id)
            is AppAction.DeleteHistoryAll -> deleteHistoryAll()
        }
    }

    private fun appendSymbol(symbol: Symbol) {
        _appState.update { state ->
            if (!state.canAppend(symbol)) return@update state

            when (symbol) {
                is Symbol.Numeric.Point -> {
                    if (state.entering.isEmpty()) {
                        state.copy(
                            result = emptyList(),
                            entering = listOf(
                                Symbol.Numeric.Digit(0),
                                symbol,
                            )
                        )
                    } else {
                        state.copy(
                            entering = if ((state.entering.lastOrNull() is Symbol.Numeric)) {
                                state.entering + symbol
                            } else {
                                state.entering + Symbol.Numeric.Digit(0) + symbol
                            }
                        )
                    }
                }

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

        when (val calculatedResult: Result<List<Symbol>, CalculatorError> =
            calculate(
                expression = state.expression,
                precision = CALCULATION_PRECISION,
                displayScale = DISPLAY_SCALE,
            )) {
            is Result.Ok -> {
                _appState.update {
                    it.copy(
                        result = calculatedResult.value,
                        entering = emptyList(),
                    )
                }

                viewModelScope.launch {
                    historyRepository.addHistory(
                        expression = state.expression,
                        result = calculatedResult.value,
                    )
                }
            }

            is Result.Err -> {
                _errorEvent.tryEmit(calculatedResult.error)
            }
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
            if (state.isEntering) {
                state.copy(
                    entering = state.entering.dropLast(1),
                )
            } else {
                state
            }
        }
    }

    private fun selectHistory(result: List<Symbol>) {
        _appState.update { state ->
            if (state.isEntering) {
                if (state.canAppend(result)) {
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

    private fun deleteHistory(id: String) {
        viewModelScope.launch {
            historyRepository.deleteHistory(id)
        }
    }

    private fun deleteHistoryAll() {
        viewModelScope.launch {
            historyRepository.deleteHistoryAll()
        }
    }
}

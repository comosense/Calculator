package com.gmail.comosense.calculator.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.data.HistoryRepository
import com.gmail.comosense.calculator.data.HistoryRepositoryError
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

enum class AppViewModelError {
    ExpressionTooLarge,
    HistoryNotFound,
}

sealed interface AppAction {
    data class Input(val symbol: Symbol) : AppAction
    data object Calculate : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
    data class SelectHistory(val id: String) : AppAction
    data class DeleteHistory(val id: String) : AppAction
    data object DeleteHistoryAll : AppAction
}

class AppViewModel(private val historyRepository: HistoryRepository) : ViewModel() {
    companion object {
        private const val CALCULATION_PRECISION: Int = 32
        private const val DISPLAY_SCALE: Int = 24
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (AppViewModel::class.java.isAssignableFrom(modelClass)) {
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

    private val _appEvent: MutableSharedFlow<AppEvent> =
        MutableSharedFlow(extraBufferCapacity = 1)
    val appEvent: SharedFlow<AppEvent> =
        _appEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            historyRepository.histories.collect { histories ->
                _appState.update { state ->
                    state.copy(histories = histories)
                }
            }
        }
    }

    fun onAction(action: AppAction) {
        when (action) {
            is AppAction.Input -> input(action.symbol)
            is AppAction.Calculate -> calculate()
            is AppAction.Clear -> clear()
            is AppAction.Backspace -> backspace()
            is AppAction.SelectHistory -> selectHistory(action.id)
            is AppAction.DeleteHistory -> deleteHistory(action.id)
            is AppAction.DeleteHistoryAll -> deleteHistoryAll()
        }
    }

    private fun input(symbol: Symbol) {
        applyUpdateResult(_appState.value.appendExpression(symbol))
    }

    private fun calculate() {
        val state: AppState = _appState.value

        if (!state.isInputting) return

        val expression: List<Symbol> = state.expression
        val result: List<Symbol> =
            when (val calculatedResult: Result<List<Symbol>, CalculatorError> =
                calculate(
                    expression = expression,
                    precision = CALCULATION_PRECISION,
                    displayScale = DISPLAY_SCALE,
                )) {
                is Result.Ok ->
                    calculatedResult.value

                is Result.Err -> {
                    _appEvent.tryEmit(AppEvent.CalculatorError(calculatedResult.error))
                    return
                }
            }

        if (!applyUpdateResult(state.applyResult(result))) return

        viewModelScope.launch {
            handleHistoryRepositoryResult(
                historyRepository.addHistory(
                    expression = expression,
                    result = result
                )
            )
        }
    }

    private fun clear() {
        _appState.update { state ->
            state.clearExpression()
        }
    }

    private fun backspace() {
        _appState.update { state ->
            state.dropLastExpressionSymbol()
        }
    }

    private fun selectHistory(id: String) {
        applyUpdateResult(_appState.value.applyHistory(id))
    }

    private fun deleteHistory(id: String) {
        viewModelScope.launch {
            handleHistoryRepositoryResult(historyRepository.deleteHistory(id))
        }
    }

    private fun deleteHistoryAll() {
        viewModelScope.launch {
            handleHistoryRepositoryResult(historyRepository.deleteHistoryAll())
        }
    }

    private fun applyUpdateResult(appStateUpdateResult: AppStateUpdateResult): Boolean {
        return when (appStateUpdateResult) {
            is AppStateUpdateResult.Success -> {
                _appState.update { appStateUpdateResult.state }
                true
            }

            is AppStateUpdateResult.ExpressionTooLarge -> {
                _appEvent.tryEmit(AppEvent.AppViewModelError(AppViewModelError.ExpressionTooLarge))
                false
            }

            is AppStateUpdateResult.UnacceptableSymbol -> {
                false
            }

            is AppStateUpdateResult.HistoryNotFound -> {
                _appEvent.tryEmit(AppEvent.AppViewModelError(AppViewModelError.HistoryNotFound))
                false
            }
        }
    }

    private fun handleHistoryRepositoryResult(historyRepositoryResult: Result<Unit, HistoryRepositoryError>) {
        when (historyRepositoryResult) {
            is Result.Ok ->
                Unit

            is Result.Err ->
                _appEvent.tryEmit(AppEvent.HistoryRepositoryError(historyRepositoryResult.error))
        }
    }
}

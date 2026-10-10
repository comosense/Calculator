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

    private data class AppStateTransition(
        val appState: AppState,
        val appEvent: AppEvent? = null,
    )

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
        updateAppState { current ->
            resolveAppStateUpdate(
                current = current,
                result = current.appendExpression(symbol)
            )
        }
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

        updateAppState { current ->
            resolveAppStateUpdate(
                current = current,
                result = current.applyResult(result)
            )
        }

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
        updateAppState { current ->
            resolveAppStateUpdate(
                current = current,
                result = current.applyHistory(id)
            )
        }
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

    private inline fun updateAppState(
        crossinline appStateTransition: (AppState) -> AppStateTransition,
    ) {
        while (true) {
            val current: AppState = _appState.value
            val next: AppStateTransition = appStateTransition(current)

            if (_appState.compareAndSet(current, next.appState)) {
                next.appEvent?.let { appEvent ->
                    _appEvent.tryEmit(appEvent)
                }
                return
            }
        }
    }

    private fun resolveAppStateUpdate(
        current: AppState,
        result: AppStateUpdateResult,
    ): AppStateTransition = when (result) {
        is AppStateUpdateResult.Success ->
            AppStateTransition(result.state)

        is AppStateUpdateResult.ExpressionTooLarge ->
            AppStateTransition(
                appState = current,
                appEvent = AppEvent.AppViewModelError(
                    AppViewModelError.ExpressionTooLarge,
                ),
            )

        is AppStateUpdateResult.UnacceptableSymbol ->
            AppStateTransition(current)

        is AppStateUpdateResult.HistoryNotFound ->
            AppStateTransition(
                appState = current,
                appEvent = AppEvent.AppViewModelError(
                    AppViewModelError.HistoryNotFound,
                ),
            )
    }

    private fun handleHistoryRepositoryResult(
        historyRepositoryResult: Result<Unit, HistoryRepositoryError>,
    ) = when (historyRepositoryResult) {
        is Result.Ok ->
            Unit

        is Result.Err ->
            _appEvent.tryEmit(AppEvent.HistoryRepositoryError(historyRepositoryResult.error))
    }
}

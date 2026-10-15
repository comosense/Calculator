package com.gmail.comosense.calculator.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.data.HistoryRepository
import com.gmail.comosense.calculator.data.HistoryRepositoryError
import com.gmail.comosense.calculator.data.Settings
import com.gmail.comosense.calculator.data.SettingsRepository
import com.gmail.comosense.calculator.data.SettingsRepositoryError
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
    HistoryNotFound,
    ExpressionTooLarge,
}

sealed interface AppAction {
    data class SetPrecision(val value: Int) : AppAction
    data class SelectHistory(val id: String) : AppAction
    data class DeleteHistory(val id: String) : AppAction
    data object DeleteHistoryAll : AppAction
    data object Calculate : AppAction
    data class Input(val symbol: Symbol) : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
}

class AppViewModel(
    private val settingRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (AppViewModel::class.java.isAssignableFrom(modelClass)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(
                    settingRepository = SettingsRepository(
                        context = application,
                    ),
                    historyRepository = HistoryRepository(
                        dataStore = application.historyDataStore,
                    ),
                ) as T
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
            settingRepository.settings.collect { settings ->
                _appState.update { appState ->
                    appState.copy(settings = settings)
                }
            }
        }

        viewModelScope.launch {
            historyRepository.histories.collect { histories ->
                _appState.update { appState ->
                    appState.copy(histories = histories)
                }
            }
        }
    }

    fun onAction(appAction: AppAction) {
        when (appAction) {
            is AppAction.SetPrecision ->
                updatePrecision(appAction.value)

            is AppAction.SelectHistory ->
                selectHistory(appAction.id)

            is AppAction.DeleteHistory ->
                deleteHistory(appAction.id)

            is AppAction.DeleteHistoryAll ->
                deleteHistoryAll()

            is AppAction.Calculate ->
                calculate()

            is AppAction.Input ->
                input(appAction.symbol)

            is AppAction.Clear ->
                clear()

            is AppAction.Backspace ->
                backspace()

        }
    }

    private fun updatePrecision(value: Int) {
        launchSettingsRepositoryAction {
            settingRepository.updatePrecision(value)
        }
    }

    private fun selectHistory(id: String) {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.applyHistory(id)
            )
        }
    }

    private fun deleteHistory(id: String) {
        launchHistoryRepositoryAction {
            historyRepository.deleteHistory(id)
        }
    }

    private fun deleteHistoryAll() {
        launchHistoryRepositoryAction {
            historyRepository.deleteHistoryAll()
        }
    }

    private fun calculate() {
        val state: AppState = _appState.value

        if (!state.isInputting) return

        val settings: Settings = state.settings
        val expression: List<Symbol> = state.expression
        val result: List<Symbol> =
            when (val calculatedResult: Result<List<Symbol>, CalculatorError> =
                calculate(
                    expression = expression,
                    precision = settings.precision,
                )) {
                is Result.Ok ->
                    calculatedResult.value

                is Result.Err -> {
                    _appEvent.tryEmit(AppEvent.CalculatorError(calculatedResult.error))
                    return
                }
            }

        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.applyResult(result),
            )
        }

        launchHistoryRepositoryAction {
            historyRepository.addHistory(
                expression = expression,
                result = result,
            )
        }
    }

    private fun input(symbol: Symbol) {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.appendExpression(symbol),
            )
        }
    }

    private fun clear() {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.clearExpression(),
            )
        }
    }

    private fun backspace() {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.dropLastExpressionSymbol(),
            )
        }
    }

    private inline fun updateAppState(
        crossinline appStateTransition: (AppState) -> AppStateTransition,
    ) {
        while (true) {
            val currentAppState: AppState = _appState.value
            val nextAppStateTransition: AppStateTransition = appStateTransition(currentAppState)

            if (_appState.compareAndSet(
                    expect = currentAppState,
                    update = nextAppStateTransition.appState,
                )
            ) {
                nextAppStateTransition.appEvent?.let { appEvent ->
                    _appEvent.tryEmit(appEvent)
                }

                return
            }
        }
    }

    private fun resolveAppStateUpdate(
        appState: AppState,
        appStateUpdateResult: AppStateUpdateResult,
    ): AppStateTransition = when (appStateUpdateResult) {
        is AppStateUpdateResult.Success ->
            AppStateTransition(
                appState = appStateUpdateResult.appState,
            )

        is AppStateUpdateResult.ExpressionTooLarge ->
            AppStateTransition(
                appState = appState,
                appEvent = AppEvent.AppViewModelError(
                    AppViewModelError.ExpressionTooLarge,
                ),
            )

        is AppStateUpdateResult.UnacceptableSymbol ->
            AppStateTransition(
                appState = appState,
            )

        is AppStateUpdateResult.HistoryNotFound ->
            AppStateTransition(
                appState = appState,
                appEvent = AppEvent.AppViewModelError(
                    AppViewModelError.HistoryNotFound,
                ),
            )
    }

    private fun launchSettingsRepositoryAction(
        action: suspend () -> Result<Unit, SettingsRepositoryError>,
    ) {
        viewModelScope.launch {
            when (val settingsRepositoryResult = action()) {
                is Result.Ok ->
                    Unit

                is Result.Err ->
                    _appEvent.tryEmit(
                        AppEvent.SettingsRepositoryError(settingsRepositoryResult.error)
                    )
            }
        }
    }

    private fun launchHistoryRepositoryAction(
        action: suspend () -> Result<Unit, HistoryRepositoryError>,
    ) {
        viewModelScope.launch {
            when (val historyRepositoryResult = action()) {
                is Result.Ok ->
                    Unit

                is Result.Err ->
                    _appEvent.tryEmit(
                        AppEvent.HistoryRepositoryError(historyRepositoryResult.error)
                    )
            }
        }
    }
}

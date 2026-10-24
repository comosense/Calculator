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
import com.gmail.comosense.calculator.data.settingsDataStore
import com.gmail.comosense.calculator.domain.CalculatorError
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.domain.calculate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppViewModelError {
    HistoryNotFound,
    UnacceptableSymbol,
    ExpressionTooLarge,
}

sealed interface AppAction {
    data class SetPrecision(val value: Int) : AppAction
    data class SelectHistory(val id: String) : AppAction
    data class DeleteHistory(val id: String) : AppAction
    data object DeleteHistoryAll : AppAction
    data object Calculate : AppAction
    data object CalculateCancel : AppAction
    data class Input(val symbol: Symbol) : AppAction
    data class Inputs(val symbols: List<Symbol>) : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
    data object ToggleAngleMode : AppAction
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
                        dataStore = application.settingsDataStore,
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
    private val _appEvent: MutableSharedFlow<AppEvent> =
        MutableSharedFlow(extraBufferCapacity = 1)
    private var calculateJobOrNull: Job? = null

    init {
        launchSettingsRepositoryAction {
            settingRepository.repairInvalidSettings()
        }

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

    val appState: StateFlow<AppState> = _appState.asStateFlow()
    val appEvent: SharedFlow<AppEvent> = _appEvent.asSharedFlow()

    fun onAppAction(appAction: AppAction) {
        if (_appState.value.isCalculating) {
            when (appAction) {
                is AppAction.SelectHistory,
                is AppAction.Calculate,
                is AppAction.Input,
                is AppAction.Inputs,
                is AppAction.Clear,
                is AppAction.Backspace,
                is AppAction.ToggleAngleMode ->
                    return

                else ->
                    Unit
            }
        }

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

            is AppAction.CalculateCancel ->
                calculateCancel()

            is AppAction.Input ->
                input(appAction.symbol)

            is AppAction.Inputs ->
                inputs(appAction.symbols)

            is AppAction.Clear ->
                clear()

            is AppAction.Backspace ->
                backspace()

            is AppAction.ToggleAngleMode ->
                toggleAngleMode()
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

        if (!state.isInputting || state.isCalculating) {
            return
        }

        val settings: Settings = state.settings
        val expression: List<Symbol> = state.expression
        val isRadians: Boolean = state.isRadians

        if (!_appState.compareAndSet(
                expect = state,
                update = state.copy(isCalculating = true)
            )
        ) return

        calculateJobOrNull = viewModelScope.launch {
            try {
                val calculatedResult: Result<List<Symbol>, CalculatorError> =
                    withContext(Dispatchers.Default) {
                        calculate(
                            expression = expression,
                            isRadians = isRadians,
                            precision = settings.precision,
                        )
                    }

                currentCoroutineContext().ensureActive()

                val result: List<Symbol> = when (calculatedResult) {
                    is Result.Ok ->
                        calculatedResult.value

                    is Result.Err -> {
                        _appEvent.tryEmit(AppEvent.CalculatorError(calculatedResult.error))
                        return@launch
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
            } finally {
                _appState.update { appState ->
                    appState.copy(isCalculating = false)
                }
            }
        }
    }

    private fun calculateCancel() {
        calculateJobOrNull?.cancel()
    }

    private fun input(symbol: Symbol) = this.inputs(listOf(symbol))
    private fun inputs(symbols: List<Symbol>) {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.appendExpression(symbols),
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

    private fun toggleAngleMode() {
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.toggleAngleMode(),
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
                appEvent = AppEvent.AppViewModelError(
                    AppViewModelError.UnacceptableSymbol,
                ),
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
        launchRepositoryAction(action) { error ->
            AppEvent.SettingsRepositoryError(error)
        }
    }

    private fun launchHistoryRepositoryAction(
        action: suspend () -> Result<Unit, HistoryRepositoryError>,
    ) {
        launchRepositoryAction(action) { error ->
            AppEvent.HistoryRepositoryError(error)
        }
    }

    private fun <E> launchRepositoryAction(
        action: suspend () -> Result<Unit, E>,
        toAppEvent: (E) -> AppEvent,
    ) {
        viewModelScope.launch {
            when (val result: Result<Unit, E> = action()) {
                is Result.Ok ->
                    Unit

                is Result.Err ->
                    _appEvent.tryEmit(toAppEvent(result.error))
            }
        }
    }
}

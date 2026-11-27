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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppViewModelError {
    HistoryNotFound,
    InvalidSymbol,
    ExpressionInsertionNotAllowed,
    ExpressionTooLarge,
}

sealed interface AppAction {
    data class SetPrecision(val value: Int) : AppAction
    data class SelectHistory(val id: String) : AppAction
    data class DeleteHistory(val id: String) : AppAction
    data object DeleteAllHistory : AppAction
    data object Calculate : AppAction
    data object CancelCalculation : AppAction
    data class Input(val symbol: Symbol) : AppAction
    data class InputMultiple(val symbols: List<Symbol>) : AppAction
    data object Clear : AppAction
    data object Backspace : AppAction
    data object ToggleAngleMode : AppAction
}

class AppViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {
    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (AppViewModel::class.java.isAssignableFrom(modelClass)) {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(
                    settingsRepository = SettingsRepository(
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
    private val _appEvent: Channel<AppEvent> =
        Channel(
            capacity = Channel.BUFFERED,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private var calculateJob: Job? = null

    init {
        launchSettingsRepositoryAction {
            settingsRepository.repairInvalidSettings()
        }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _appState.update { appState ->
                    appState.copy(
                        settings = settings,
                    )
                }
            }
        }

        viewModelScope.launch {
            historyRepository.histories.collect { histories ->
                _appState.update { appState ->
                    appState.copy(
                        histories = histories,
                    )
                }
            }
        }
    }

    val appState: StateFlow<AppState> = _appState.asStateFlow()
    val appEvent: Flow<AppEvent> = _appEvent.receiveAsFlow()

    override fun onCleared() {
        super.onCleared()
        _appEvent.close()
    }

    fun onAppAction(
        appAction: AppAction,
    ) {
        if (_appState.value.isCalculating &&
            !appAction.isAllowedWhileCalculating()
        ) {
            return
        }

        when (appAction) {
            is AppAction.SetPrecision ->
                updatePrecision(appAction.value)

            is AppAction.SelectHistory ->
                selectHistory(appAction.id)

            is AppAction.DeleteHistory ->
                deleteHistory(appAction.id)

            is AppAction.DeleteAllHistory ->
                deleteAllHistory()

            is AppAction.Calculate ->
                calculate()

            is AppAction.CancelCalculation ->
                cancelCalculation()

            is AppAction.Input ->
                input(appAction.symbol)

            is AppAction.InputMultiple ->
                inputMultiple(appAction.symbols)

            is AppAction.Clear ->
                clear()

            is AppAction.Backspace ->
                backspace()

            is AppAction.ToggleAngleMode ->
                toggleAngleMode()
        }
    }

    private fun AppAction.isAllowedWhileCalculating(): Boolean =
        when (this) {
            is AppAction.CancelCalculation,
            is AppAction.DeleteHistory,
            is AppAction.DeleteAllHistory,
            is AppAction.SetPrecision -> true

            else -> false
        }

    private fun updatePrecision(
        value: Int,
    ) =
        launchSettingsRepositoryAction {
            settingsRepository.updatePrecision(value)
        }

    private fun selectHistory(
        id: String,
    ) =
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.applyHistory(id),
            )
        }

    private fun deleteHistory(
        id: String,
    ) =
        launchHistoryRepositoryAction {
            historyRepository.deleteHistory(id)
        }

    private fun deleteAllHistory() =
        launchHistoryRepositoryAction {
            historyRepository.deleteAllHistory()
        }

    private fun calculate() {
        val state: AppState = _appState.value

        if (!state.hasInput() || state.isCalculating) {
            return
        }

        val settings: Settings = state.settings
        val expression: List<Symbol> = state.expression
        val isRadians: Boolean = state.isRadians

        if (!_appState.compareAndSet(
                expect = state,
                update = state.copy(
                    isCalculating = true,
                ),
            )
        ) return

        val job: Job = viewModelScope.launch(
            start = CoroutineStart.LAZY,
        ) {
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
                        _appEvent.trySend(AppEvent.CalculatorError(calculatedResult.error))
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
                finishCalculation(coroutineContext[Job])
            }
        }

        calculateJob = job
        job.start()
    }

    private fun cancelCalculation() =
        calculateJob?.cancel()

    private fun finishCalculation(job: Job?) {
        if (calculateJob !== job) {
            return
        }

        calculateJob = null

        _appState.update { appState ->
            appState.copy(
                isCalculating = false,
            )
        }
    }

    private fun input(
        symbol: Symbol,
    ) =
        inputMultiple(listOf(symbol))

    private fun inputMultiple(
        symbols: List<Symbol>,
    ) =
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.appendExpression(symbols),
            )
        }

    private fun clear() =
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.clearExpression(),
            )
        }

    private fun backspace() =
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.dropLastExpressionSymbol(),
            )
        }

    private fun toggleAngleMode() =
        updateAppState { appState ->
            resolveAppStateUpdate(
                appState = appState,
                appStateUpdateResult = appState.toggleAngleMode(),
            )
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
                    _appEvent.trySend(appEvent)
                }

                return
            }
        }
    }

    private fun resolveAppStateUpdate(
        appState: AppState,
        appStateUpdateResult: AppStateUpdateResult,
    ): AppStateTransition =
        when (appStateUpdateResult) {
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

            is AppStateUpdateResult.InvalidSymbol ->
                AppStateTransition(
                    appState = appState,
                    appEvent = AppEvent.AppViewModelError(
                        AppViewModelError.InvalidSymbol,
                    ),
                )

            is AppStateUpdateResult.ExpressionInsertionNotAllowed ->
                AppStateTransition(
                    appState = appState,
                    appEvent = AppEvent.AppViewModelError(
                        AppViewModelError.ExpressionInsertionNotAllowed,
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
    ) =
        launchRepositoryAction(action) { error ->
            AppEvent.SettingsRepositoryError(error)
        }

    private fun launchHistoryRepositoryAction(
        action: suspend () -> Result<Unit, HistoryRepositoryError>,
    ) =
        launchRepositoryAction(action) { error ->
            AppEvent.HistoryRepositoryError(error)
        }

    private fun <E> launchRepositoryAction(
        action: suspend () -> Result<Unit, E>,
        toAppEvent: (E) -> AppEvent,
    ) =
        viewModelScope.launch {
            when (val result: Result<Unit, E> = action()) {
                is Result.Ok ->
                    Unit

                is Result.Err ->
                    _appEvent.trySend(toAppEvent(result.error))
            }
        }
}

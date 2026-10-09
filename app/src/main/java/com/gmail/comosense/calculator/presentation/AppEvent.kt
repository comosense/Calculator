package com.gmail.comosense.calculator.presentation

sealed interface AppEvent {
    data class CalculatorError(
        val calculatorError: com.gmail.comosense.calculator.domain.CalculatorError
    ) : AppEvent

    data class HistoryRepositoryError(
        val historyRepositoryError: com.gmail.comosense.calculator.data.HistoryRepositoryError
    ) : AppEvent
}

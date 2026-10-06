package com.gmail.comosense.calculator.presentation

sealed interface AppEvent {
    data class CalculatorError(
        val calculatorError: com.gmail.comosense.calculator.domain.CalculatorError
    ) : AppEvent

    data class HistoryError(
        val historyError: com.gmail.comosense.calculator.data.HistoryError
    ) : AppEvent
}

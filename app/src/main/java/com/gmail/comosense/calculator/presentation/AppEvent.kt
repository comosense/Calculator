package com.gmail.comosense.calculator.presentation

sealed interface AppEvent {
    data class CalculatorError(
        val error: com.gmail.comosense.calculator.domain.CalculatorError
    ) : AppEvent
}

package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.data.History
import com.gmail.comosense.calculator.data.Settings
import com.gmail.comosense.calculator.domain.Symbol

data class AppState(
    val settings: Settings = Settings(),
    val histories: List<History> = emptyList(),
    val result: List<Symbol> = emptyList(),
    val input: List<Symbol> = emptyList(),
    val isRadians: Boolean = true,
    val isCalculating: Boolean = false,
)

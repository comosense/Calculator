package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.data.History
import com.gmail.comosense.calculator.domain.Symbol

data class AppState(
    val result: List<Symbol> = emptyList(),
    val input: List<Symbol> = emptyList(),
    val histories: List<History> = emptyList(),
)

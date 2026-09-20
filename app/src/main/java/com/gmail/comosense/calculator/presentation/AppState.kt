package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Calculation
import com.gmail.comosense.calculator.domain.Symbol

data class AppState(
    val result: List<Symbol> = emptyList(),
    val entering: List<Symbol> = emptyList(),
    val history: List<Calculation> = emptyList(),
)

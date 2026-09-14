package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

data class Calculation(val expression: List<Symbol>, val result: List<Symbol>)

data class AppState(
    val result: List<Symbol> = emptyList(),
    val entering: List<Symbol> = emptyList(),
    val history: List<Calculation> = emptyList(),
)

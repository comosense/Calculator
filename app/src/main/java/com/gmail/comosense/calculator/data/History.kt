package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.domain.Symbol

data class History(
    val id: String,
    val expression: List<Symbol>,
    val result: List<Symbol>,
)

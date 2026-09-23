package com.gmail.comosense.calculator.domain

data class Calculation(
    val id: String,
    val expression: List<Symbol>,
    val result: List<Symbol>,
)

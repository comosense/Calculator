package com.gmail.comosense.calculator.domain

data class Calculation(
    val expression: List<Symbol>,
    val result: List<Symbol>,
)

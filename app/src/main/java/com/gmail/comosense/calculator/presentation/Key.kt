package com.gmail.comosense.calculator.presentation

sealed interface Key {
    data object Positive : Key
    data object Negative : Key
    data class Digit(val value: Int) : Key
    data object Point : Key
    data object OpeningParenthesis : Key
    data object ClosingParenthesis : Key
    data object Add : Key
    data object Subtract : Key
    data object Multiply : Key
    data object Divide : Key
    data object Equal : Key
    data object Clear : Key
    data object Backspace : Key
    data object Operators : Key
}

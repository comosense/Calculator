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
    data object Power : Key
    data object Factorial : Key
    data object Equal : Key
    data object Clear : Key
    data object Backspace : Key
    data class FunctionMode(val isInversed: Boolean) : Key
    data class AngleMode(val isRadians: Boolean) : Key
    data object Euler : Key
    data object Pi : Key
    data object Sqrt : Key
    data object Ln : Key
    data object Log : Key
    data object Sin : Key
    data object Cos : Key
    data object Tan : Key
    data object Arcsin : Key
    data object Arccos : Key
    data object Arctan : Key
}

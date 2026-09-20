package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

sealed interface Key
sealed class SymbolKey : Key {
    abstract val symbol: Symbol

    data object Positive : SymbolKey() {
        override val symbol: Symbol = Symbol.Sign.Positive
    }

    data object Negative : SymbolKey() {
        override val symbol: Symbol = Symbol.Sign.Negative
    }

    data class Digit(val value: Int) : SymbolKey() {
        override val symbol: Symbol = Symbol.Numeric.Digit(value)
    }

    data object Point : SymbolKey() {
        override val symbol: Symbol = Symbol.Numeric.Point
    }

    data object OpenParenthesis : SymbolKey() {
        override val symbol: Symbol = Symbol.FactorStart.OpenParenthesis
    }

    data object CloseParenthesis : SymbolKey() {
        override val symbol: Symbol = Symbol.FactorEnd.CloseParenthesis
    }

    data object Add : SymbolKey() {
        override val symbol: Symbol = Symbol.Operator.Add
    }

    data object Subtract : SymbolKey() {
        override val symbol: Symbol = Symbol.Operator.Subtract
    }

    data object Multiply : SymbolKey() {
        override val symbol: Symbol = Symbol.Operator.Multiply
    }

    data object Divide : SymbolKey() {
        override val symbol: Symbol = Symbol.Operator.Divide
    }
}

sealed class CommandKey : Key {
    data object Equal : CommandKey()
    data object Clear : CommandKey()
    data object Backspace : CommandKey()
}

sealed class UiKey : Key {
    data object Operators : UiKey()
}

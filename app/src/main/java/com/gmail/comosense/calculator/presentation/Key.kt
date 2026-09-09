package com.gmail.comosense.calculator.presentation

enum class Style {
    Numeric,
    Operator,
    Command,
}

sealed interface Key {
    val longClickKey: Key?
    val style: Style
}

sealed class SymbolKey : Key {
    abstract val symbol: Symbol

    override val longClickKey: Key? = null

    data object Positive : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol = Symbol.Sign.Positive
    }

    data object Negative : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol = Symbol.Sign.Negative
    }

    data class Digit(val value: Int) : SymbolKey() {
        override val style: Style = Style.Numeric
        override val symbol = Symbol.Numeric.Digit(value)
    }

    data object Point : SymbolKey() {
        override val style: Style = Style.Numeric
        override val symbol = Symbol.Numeric.Point
    }

    data object OpenParenthesis : SymbolKey() {
        override val style: Style = Style.Numeric
        override val symbol = Symbol.FactorStart.OpenParenthesis
    }

    data object CloseParenthesis : SymbolKey() {
        override val style: Style = Style.Numeric
        override val symbol = Symbol.FactorEnd.CloseParenthesis
    }

    data object Add : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol: Symbol = Symbol.Operator.Add
    }

    data object Subtract : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol: Symbol = Symbol.Operator.Subtract
    }

    data object Multiply : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol: Symbol = Symbol.Operator.Multiply
    }

    data object Divide : SymbolKey() {
        override val style: Style = Style.Operator
        override val symbol: Symbol = Symbol.Operator.Divide
    }
}

sealed class CommandKey : Key {
    data object Equals : CommandKey() {
        override val longClickKey: Key? = null
        override val style: Style = Style.Command
    }

    data object Clear : CommandKey() {
        override val longClickKey: Key? = null
        override val style: Style = Style.Command
    }

    data object Delete : CommandKey() {
        override val longClickKey: Key = Clear
        override val style: Style = Style.Command
    }
}

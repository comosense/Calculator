package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

sealed interface Key {
    val appActionOrNull: AppAction?

    data object Positive : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Sign.Positive)
    }

    data object Negative : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Sign.Negative)
    }

    data class Digit(val value: Int) : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Numeric.Digit(value))
    }

    data object Point : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Numeric.Point)
    }

    data object OpeningParenthesis : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.FactorStart.OpeningParenthesis)
    }

    data object ClosingParenthesis : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.FactorEnd.ClosingParenthesis)
    }

    data object Add : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Operator.Add)
    }

    data object Subtract : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Operator.Subtract)
    }

    data object Multiply : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Operator.Multiply)
    }

    data object Divide : Key {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.Operator.Divide)
    }

    data object Equal : Key {
        override val appActionOrNull: AppAction =
            AppAction.Calculate
    }

    data object Clear : Key {
        override val appActionOrNull: AppAction =
            AppAction.Clear
    }

    data object Backspace : Key {
        override val appActionOrNull: AppAction =
            AppAction.Backspace
    }

    data object Operators : Key {
        override val appActionOrNull: AppAction? =
            null
    }
}

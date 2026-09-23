package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.domain.Symbol

sealed interface Key {
    val appActionOrNull: AppAction?
}

sealed class AppKey : Key {
    data object Positive : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Sign.Positive)
    }

    data object Negative : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Sign.Negative)
    }

    data class Digit(val value: Int) : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Numeric.Digit(value))
    }

    data object Point : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Numeric.Point)
    }

    data object OpeningParenthesis : AppKey() {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.FactorStart.OpeningParenthesis)
    }

    data object ClosingParenthesis : AppKey() {
        override val appActionOrNull: AppAction =
            AppAction.Input(Symbol.FactorEnd.ClosingParenthesis)
    }

    data object Add : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Operator.Add)
    }

    data object Subtract : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Operator.Subtract)
    }

    data object Multiply : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Operator.Multiply)
    }

    data object Divide : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Input(Symbol.Operator.Divide)
    }

    data object Equal : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Calculate
    }

    data object Clear : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Clear
    }

    data object Backspace : AppKey() {
        override val appActionOrNull: AppAction = AppAction.Backspace
    }
}

sealed class UiKey : Key {
    data object Operators : UiKey() {
        override val appActionOrNull: AppAction? = null
    }
}

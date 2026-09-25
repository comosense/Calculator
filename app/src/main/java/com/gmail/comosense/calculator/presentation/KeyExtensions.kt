package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.domain.Symbol

enum class Style {
    Digit,
    Operator,
    Command,
    Ui,
}

interface Display {
    data class Text(
        val text: String,
    ) : Display

    data class Icon(
        val painterResource: Int,
        val stringResource: Int,
    ) : Display
}

val Key.style: Style
    get() {
        return when (this) {
            is Key.Digit,
            is Key.Point,
            is Key.OpeningParenthesis,
            is Key.ClosingParenthesis ->
                Style.Digit

            is Key.Positive,
            is Key.Negative,
            is Key.Add,
            is Key.Subtract,
            is Key.Multiply,
            is Key.Divide ->
                Style.Operator

            is Key.Equal,
            is Key.Clear,
            is Key.Backspace ->
                Style.Command

            is Key.Operators ->
                Style.Ui
        }
    }

fun Key.display(symbolFormatter: SymbolFormatter): Display = when (this) {
    is Key.Positive ->
        Display.Icon(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.positive,
        )

    is Key.Negative ->
        Display.Icon(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.negative,
        )

    is Key.Digit ->
        Display.Text(value.toString())

    is Key.Point ->
        Display.Text(symbolFormatter.decimalSeparator.toString())

    is Key.OpeningParenthesis ->
        Display.Icon(
            painterResource = R.drawable.ic_opening_parenthesis,
            stringResource = R.string.opening_parenthesis,
        )

    is Key.ClosingParenthesis ->
        Display.Icon(
            painterResource = R.drawable.ic_closing_parenthesis,
            stringResource = R.string.closing_parenthesis,
        )

    is Key.Add ->
        Display.Icon(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.add,
        )

    is Key.Subtract ->
        Display.Icon(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.subtract,
        )

    is Key.Multiply ->
        Display.Icon(
            painterResource = R.drawable.ic_multiply,
            stringResource = R.string.multiply,
        )

    is Key.Divide ->
        Display.Icon(
            painterResource = R.drawable.ic_divide,
            stringResource = R.string.divide,
        )

    is Key.Equal ->
        Display.Icon(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.equal,
        )

    is Key.Clear ->
        Display.Icon(
            painterResource = R.drawable.ic_clear,
            stringResource = R.string.clear,
        )

    is Key.Backspace ->
        Display.Icon(
            painterResource = R.drawable.ic_backspace,
            stringResource = R.string.backspace,
        )

    is Key.Operators ->
        Display.Icon(
            painterResource = R.drawable.ic_operators,
            stringResource = R.string.operators,
        )
}

val Key.appActionOrNull: AppAction?
    get() = when (this) {
        is Key.Positive ->
            AppAction.Input(Symbol.Sign.Positive)

        is Key.Negative ->
            AppAction.Input(Symbol.Sign.Negative)

        is Key.Digit ->
            AppAction.Input(Symbol.Numeric.Digit(value))

        is Key.Point ->
            AppAction.Input(Symbol.Numeric.Point)

        is Key.OpeningParenthesis ->
            AppAction.Input(Symbol.FactorStart.OpeningParenthesis)

        is Key.ClosingParenthesis ->
            AppAction.Input(Symbol.FactorEnd.ClosingParenthesis)

        is Key.Add ->
            AppAction.Input(Symbol.Operator.Add)

        is Key.Subtract ->
            AppAction.Input(Symbol.Operator.Subtract)

        is Key.Multiply ->
            AppAction.Input(Symbol.Operator.Multiply)

        is Key.Divide ->
            AppAction.Input(Symbol.Operator.Divide)

        is Key.Equal ->
            AppAction.Calculate

        is Key.Clear ->
            AppAction.Clear

        is Key.Backspace ->
            AppAction.Backspace

        else ->
            null
    }

val Key.longClickKeyOrNull: Key?
    get() = when (this) {
        is Key.Backspace -> Key.Clear
        else -> null
    }

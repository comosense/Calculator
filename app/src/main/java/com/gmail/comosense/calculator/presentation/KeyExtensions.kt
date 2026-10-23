package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.domain.Symbol

enum class Style {
    Digit,
    Operator,
    Command,
    Mode,
}

enum class UiAction {
    ToggleFunctionMode,
}

sealed interface Action {
    data class App(val appAction: AppAction) : Action
    data class Ui(val uiAction: UiAction) : Action
}

sealed interface Display {
    data class Text(
        val text: String,
    ) : Display

    data class Icon(
        val painterResource: Int,
        val stringResource: Int,
    ) : Display
}

val Key.action: Action
    get() = when (this) {
        is Key.Positive ->
            Action.App(AppAction.Input(Symbol.Sign.Positive))

        is Key.Negative ->
            Action.App(AppAction.Input(Symbol.Sign.Negative))

        is Key.Digit ->
            Action.App(AppAction.Input(Symbol.Numeric.Digit(value)))

        is Key.Point ->
            Action.App(AppAction.Input(Symbol.Numeric.Point))

        is Key.OpeningParenthesis ->
            Action.App(AppAction.Input(Symbol.FactorStart.OpeningParenthesis))

        is Key.ClosingParenthesis ->
            Action.App(AppAction.Input(Symbol.FactorEnd.ClosingParenthesis))

        is Key.Add ->
            Action.App(AppAction.Input(Symbol.Operator.Add))

        is Key.Subtract ->
            Action.App(AppAction.Input(Symbol.Operator.Subtract))

        is Key.Multiply ->
            Action.App(AppAction.Input(Symbol.Operator.Multiply))

        is Key.Divide ->
            Action.App(AppAction.Input(Symbol.Operator.Divide))

        is Key.Power ->
            Action.App(AppAction.Input(Symbol.SpecialOperator.Power))

        is Key.Factorial ->
            Action.App(AppAction.Input(Symbol.SpecialOperator.Factorial))

        is Key.Equal ->
            Action.App(AppAction.Calculate)

        is Key.Clear ->
            Action.App(AppAction.Clear)

        is Key.Backspace ->
            Action.App(AppAction.Backspace)

        is Key.FunctionMode ->
            Action.Ui(UiAction.ToggleFunctionMode)

        is Key.AngleMode ->
            Action.App(AppAction.ToggleAngleMode)

        is Key.Euler ->
            Action.App(AppAction.Input(Symbol.Constant.Euler))

        is Key.Pi ->
            Action.App(AppAction.Input(Symbol.Constant.Pi))

        is Key.Sqrt ->
            Action.App(AppAction.Input(Symbol.Factor.Function.Sqrt))

        is Key.Ln ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Ln))

        is Key.Log ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Log))

        is Key.Sin ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Sin))

        is Key.Cos ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Cos))

        is Key.Tan ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Tan))

        is Key.Arcsin ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Arcsin))

        is Key.Arccos ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Arccos))

        is Key.Arctan ->
            Action.App(AppAction.Input(Symbol.FactorStart.Function.Arctan))
    }

val Key.altKeyOrNull: Key?
    get() = when (this) {
        is Key.ClosingParenthesis
            -> Key.OpeningParenthesis

        is Key.Backspace
            -> Key.Clear

        else
            -> null
    }

val Key.style: Style
    get() = when (this) {
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

        is Key.Power,
        is Key.Factorial,
        is Key.Euler,
        is Key.Pi,
        is Key.Sqrt,
        is Key.Ln,
        is Key.Log,
        is Key.Sin,
        is Key.Cos,
        is Key.Tan,
        is Key.Arcsin,
        is Key.Arccos,
        is Key.Arctan ->
            Style.Digit

        is Key.FunctionMode,
        is Key.AngleMode ->
            Style.Mode
    }

fun Key.display(symbolFormatter: SymbolFormatter): Display = when (this) {
    is Key.Positive ->
        Display.Icon(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.content_description_positive,
        )

    is Key.Negative ->
        Display.Icon(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.content_description_negative,
        )

    is Key.Digit ->
        Display.Text(value.toString())

    is Key.Point ->
        Display.Text(symbolFormatter.decimalSeparator.toString())

    is Key.OpeningParenthesis ->
        Display.Icon(
            painterResource = R.drawable.ic_opening_parenthesis,
            stringResource = R.string.content_description_opening_parenthesis,
        )

    is Key.ClosingParenthesis ->
        Display.Icon(
            painterResource = R.drawable.ic_closing_parenthesis,
            stringResource = R.string.content_description_closing_parenthesis,
        )

    is Key.Add ->
        Display.Icon(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.content_description_add,
        )

    is Key.Subtract ->
        Display.Icon(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.content_description_subtract,
        )

    is Key.Multiply ->
        Display.Icon(
            painterResource = R.drawable.ic_multiply,
            stringResource = R.string.content_description_multiply,
        )

    is Key.Divide ->
        Display.Icon(
            painterResource = R.drawable.ic_divide,
            stringResource = R.string.content_description_divide,
        )

    is Key.Power ->
        Display.Text("^")

    is Key.Factorial ->
        Display.Text("!")

    is Key.Equal ->
        Display.Icon(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.content_description_equal,
        )

    is Key.Clear ->
        Display.Icon(
            painterResource = R.drawable.ic_clear,
            stringResource = R.string.content_description_clear,
        )

    is Key.Backspace ->
        Display.Icon(
            painterResource = R.drawable.ic_backspace,
            stringResource = R.string.content_description_backspace,
        )

    is Key.FunctionMode ->
        Display.Icon(
            painterResource = if (isInverseFunctionMode) {
                R.drawable.ic_inverse_function
            } else {
                R.drawable.ic_function
            },
            stringResource = R.string.content_description_function_mode,
        )

    is Key.AngleMode ->
        Display.Icon(
            painterResource = if (isRadians) {
                R.drawable.ic_radians
            } else {
                R.drawable.ic_degrees
            },
            stringResource = R.string.content_description_angle_mode,
        )

    is Key.Euler ->
        Display.Text("e")

    is Key.Pi ->
        Display.Text("π")

    is Key.Sqrt ->
        Display.Text("√")

    is Key.Ln ->
        Display.Text("ln")

    is Key.Log ->
        Display.Text("log")

    is Key.Sin ->
        Display.Text("sin")

    is Key.Cos ->
        Display.Text("cos")

    is Key.Tan ->
        Display.Text("tan")

    is Key.Arcsin ->
        Display.Text("asin")

    is Key.Arccos ->
        Display.Text("acos")

    is Key.Arctan ->
        Display.Text("atan")
}

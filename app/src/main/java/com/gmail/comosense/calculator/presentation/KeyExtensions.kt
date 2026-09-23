package com.gmail.comosense.calculator.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.presentation.theme.CalculatorScreenColors
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import java.util.Locale

interface Display {
    data class Text(
        val text: String,
    ) : Display

    data class Drawable(
        val painterResource: Int,
        val stringResource: Int,
    ) : Display
}

fun Key.display(locale: Locale): Display = when (this) {
    is SymbolKey.Positive ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.positive,
        )

    is SymbolKey.Negative ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.negative,
        )

    is SymbolKey.Digit ->
        Display.Text(value.toString())

    is SymbolKey.Point ->
        Display.Text(decimalSeparator(locale).toString())

    is SymbolKey.OpeningParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_opening_parenthesis,
            stringResource = R.string.opening_parenthesis,
        )

    is SymbolKey.ClosingParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_closing_parenthesis,
            stringResource = R.string.closing_parenthesis,
        )

    is SymbolKey.Add ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.add,
        )

    is SymbolKey.Subtract ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.subtract,
        )

    is SymbolKey.Multiply ->
        Display.Drawable(
            painterResource = R.drawable.ic_multiply,
            stringResource = R.string.multiply,
        )

    is SymbolKey.Divide ->
        Display.Drawable(
            painterResource = R.drawable.ic_divide,
            stringResource = R.string.divide,
        )

    is CommandKey.Equal ->
        Display.Drawable(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.equal,
        )

    is CommandKey.Clear ->
        Display.Drawable(
            painterResource = R.drawable.ic_clear,
            stringResource = R.string.clear,
        )

    is CommandKey.Backspace ->
        Display.Drawable(
            painterResource = R.drawable.ic_backspace,
            stringResource = R.string.backspace,
        )

    is UiKey.Operators ->
        Display.Drawable(
            painterResource = R.drawable.ic_operators,
            stringResource = R.string.operators,
        )
}

val Key.longClickKeyOrNull: Key?
    get() = when (this) {
        is CommandKey.Backspace -> CommandKey.Clear
        else -> null
    }

val Key.colors: ButtonColors
    @Composable
    get() {
        val colors: CalculatorScreenColors = CalculatorTheme.calculatorScreenColors

        return when (this) {
            is SymbolKey.Digit,
            is SymbolKey.Point,
            is SymbolKey.OpeningParenthesis,
            is SymbolKey.ClosingParenthesis ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.digitKeyContainer,
                    contentColor = colors.digitKeyContent,
                )

            is SymbolKey.Positive,
            is SymbolKey.Negative,
            is SymbolKey.Add,
            is SymbolKey.Subtract,
            is SymbolKey.Multiply,
            is SymbolKey.Divide ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.operatorKeyContainer,
                    contentColor = colors.operatorKeyContent,
                )

            is CommandKey.Equal,
            is CommandKey.Clear,
            is CommandKey.Backspace ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.commandKeyContainer,
                    contentColor = colors.commandKeyContent,
                )

            is UiKey.Operators ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.uiKeyContainer,
                    contentColor = colors.uiKeyContent,
                )
        }
    }

val Key.toAppActionOrNull: AppAction?
    get() = when (this) {
        is SymbolKey -> AppAction.Input(symbol)
        is CommandKey.Equal -> AppAction.Calculate
        is CommandKey.Clear -> AppAction.Clear
        is CommandKey.Backspace -> AppAction.Backspace
        else -> null
    }

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
    is AppKey.Positive ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.positive,
        )

    is AppKey.Negative ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.negative,
        )

    is AppKey.Digit ->
        Display.Text(value.toString())

    is AppKey.Point ->
        Display.Text(decimalSeparator(locale).toString())

    is AppKey.OpeningParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_opening_parenthesis,
            stringResource = R.string.opening_parenthesis,
        )

    is AppKey.ClosingParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_closing_parenthesis,
            stringResource = R.string.closing_parenthesis,
        )

    is AppKey.Add ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.add,
        )

    is AppKey.Subtract ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.subtract,
        )

    is AppKey.Multiply ->
        Display.Drawable(
            painterResource = R.drawable.ic_multiply,
            stringResource = R.string.multiply,
        )

    is AppKey.Divide ->
        Display.Drawable(
            painterResource = R.drawable.ic_divide,
            stringResource = R.string.divide,
        )

    is AppKey.Equal ->
        Display.Drawable(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.equal,
        )

    is AppKey.Clear ->
        Display.Drawable(
            painterResource = R.drawable.ic_clear,
            stringResource = R.string.clear,
        )

    is AppKey.Backspace ->
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
        is AppKey.Backspace -> AppKey.Clear
        else -> null
    }

val Key.colors: ButtonColors
    @Composable
    get() {
        val colors: CalculatorScreenColors = CalculatorTheme.calculatorScreenColors

        return when (this) {
            is AppKey.Digit,
            is AppKey.Point,
            is AppKey.OpeningParenthesis,
            is AppKey.ClosingParenthesis ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.digitKeyContainer,
                    contentColor = colors.digitKeyContent,
                )

            is AppKey.Positive,
            is AppKey.Negative,
            is AppKey.Add,
            is AppKey.Subtract,
            is AppKey.Multiply,
            is AppKey.Divide ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.operatorKeyContainer,
                    contentColor = colors.operatorKeyContent,
                )

            is AppKey.Equal,
            is AppKey.Clear,
            is AppKey.Backspace ->
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

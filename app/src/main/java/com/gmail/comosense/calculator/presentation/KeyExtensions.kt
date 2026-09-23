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
    is Key.Positive ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.positive,
        )

    is Key.Negative ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.negative,
        )

    is Key.Digit ->
        Display.Text(value.toString())

    is Key.Point ->
        Display.Text(decimalSeparator(locale).toString())

    is Key.OpeningParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_opening_parenthesis,
            stringResource = R.string.opening_parenthesis,
        )

    is Key.ClosingParenthesis ->
        Display.Drawable(
            painterResource = R.drawable.ic_closing_parenthesis,
            stringResource = R.string.closing_parenthesis,
        )

    is Key.Add ->
        Display.Drawable(
            painterResource = R.drawable.ic_add,
            stringResource = R.string.add,
        )

    is Key.Subtract ->
        Display.Drawable(
            painterResource = R.drawable.ic_subtract,
            stringResource = R.string.subtract,
        )

    is Key.Multiply ->
        Display.Drawable(
            painterResource = R.drawable.ic_multiply,
            stringResource = R.string.multiply,
        )

    is Key.Divide ->
        Display.Drawable(
            painterResource = R.drawable.ic_divide,
            stringResource = R.string.divide,
        )

    is Key.Equal ->
        Display.Drawable(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.equal,
        )

    is Key.Clear ->
        Display.Drawable(
            painterResource = R.drawable.ic_clear,
            stringResource = R.string.clear,
        )

    is Key.Backspace ->
        Display.Drawable(
            painterResource = R.drawable.ic_backspace,
            stringResource = R.string.backspace,
        )

    is Key.Operators ->
        Display.Drawable(
            painterResource = R.drawable.ic_operators,
            stringResource = R.string.operators,
        )
}

val Key.longClickKeyOrNull: Key?
    get() = when (this) {
        is Key.Backspace -> Key.Clear
        else -> null
    }

val Key.colors: ButtonColors
    @Composable
    get() {
        val colors: CalculatorScreenColors = CalculatorTheme.calculatorScreenColors

        return when (this) {
            is Key.Digit,
            is Key.Point,
            is Key.OpeningParenthesis,
            is Key.ClosingParenthesis ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.digitKeyContainer,
                    contentColor = colors.digitKeyContent,
                )

            is Key.Positive,
            is Key.Negative,
            is Key.Add,
            is Key.Subtract,
            is Key.Multiply,
            is Key.Divide ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.operatorKeyContainer,
                    contentColor = colors.operatorKeyContent,
                )

            is Key.Equal,
            is Key.Clear,
            is Key.Backspace ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.commandKeyContainer,
                    contentColor = colors.commandKeyContent,
                )

            is Key.Operators ->
                ButtonDefaults.buttonColors(
                    containerColor = colors.uiKeyContainer,
                    contentColor = colors.uiKeyContent,
                )
        }
    }

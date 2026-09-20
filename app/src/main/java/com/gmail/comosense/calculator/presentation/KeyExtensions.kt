package com.gmail.comosense.calculator.presentation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.gmail.comosense.calculator.R
import java.text.DecimalFormatSymbols
import java.util.Locale

interface Display {
    data class Text(val text: String) : Display
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

    is SymbolKey.Point ->
        Display.Text(
            DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
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

    is SymbolKey ->
        Display.Text(symbol.text)

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

val Key.longClickKey: Key?
    get() = when (this) {
        is CommandKey.Backspace -> CommandKey.Clear
        else -> null
    }

val Key.colors: ButtonColors
    @Composable
    get() = when (this) {
        is SymbolKey.Digit,
        is SymbolKey.Point,
        is SymbolKey.OpenParenthesis,
        is SymbolKey.CloseParenthesis ->
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )

        is SymbolKey.Positive,
        is SymbolKey.Negative,
        is SymbolKey.Add,
        is SymbolKey.Subtract,
        is SymbolKey.Multiply,
        is SymbolKey.Divide ->
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )

        is CommandKey.Equal,
        is CommandKey.Clear,
        is CommandKey.Backspace ->
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )

        is UiKey.Operators ->
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )
    }

val Key.toAppAction: AppAction?
    get() = when (this) {
        is SymbolKey -> AppAction.Input(symbol)
        is CommandKey.Equal -> AppAction.Calculate
        is CommandKey.Clear -> AppAction.Clear
        is CommandKey.Backspace -> AppAction.Backspace
        else -> null
    }

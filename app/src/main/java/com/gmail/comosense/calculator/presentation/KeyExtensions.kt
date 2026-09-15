package com.gmail.comosense.calculator.presentation

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
    is SymbolKey.Point ->
        Display.Text(
            DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
        )

    is SymbolKey ->
        Display.Text(symbol.text)

    is CommandKey.Equals ->
        Display.Drawable(
            painterResource = R.drawable.ic_equal,
            stringResource = R.string.equals,
        )

    is CommandKey.Clear ->
        Display.Drawable(
            painterResource = R.drawable.ic_refresh,
            stringResource = R.string.clear,
        )

    is CommandKey.Delete ->
        Display.Drawable(
            painterResource = R.drawable.ic_keyboard_backspace,
            stringResource = R.string.delete,
        )

    is ActionKey.OperatorBox ->
        Display.Drawable(
            painterResource = R.drawable.ic_operator_key_box,
            stringResource = R.string.operator_box,
        )
}

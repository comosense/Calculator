package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.R
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed interface Display {
    data class Text(val text: String) : Display
    data class Drawable(val id: Int) : Display
}

fun Key.display(locale: Locale): Display = when (this) {
    is SymbolKey.Point ->
        Display.Text(
            DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
        )

    is SymbolKey ->
        Display.Text(symbol.toString)

    is CommandKey.Equals ->
        Display.Drawable(R.drawable.ic_equal)

    is CommandKey.Clear ->
        Display.Drawable(R.drawable.ic_refresh)

    is CommandKey.Delete ->
        Display.Drawable(R.drawable.ic_keyboard_backspace)

    is ActionKey.OperatorBox ->
        Display.Drawable(R.drawable.ic_operator_key_box)
}

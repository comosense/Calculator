package com.gmail.comosense.calculator.common

import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

fun formatNumericString(value: String, locale: Locale): String {
    val pointIndex: Int = value.indexOf(SymbolText.POINT)
    val integerPart: String = if (pointIndex >= 0) {
        value.substring(0, pointIndex)
    } else {
        value
    }

    val decimalPart: String? = if (pointIndex >= 0) {
        value.substring(pointIndex + 1)
    } else {
        null
    }

    val formattedIntegerPart = formatIntegerPart(
        integerPart = integerPart,
        locale = locale,
    )

    return if (decimalPart == null) {
        formattedIntegerPart
    } else {
        buildString {
            append(formattedIntegerPart)
            append(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
            append(decimalPart)
        }
    }
}

private fun formatIntegerPart(integerPart: String, locale: Locale): String {
    if (integerPart.isEmpty()) return integerPart

    val formatter: DecimalFormat =
        NumberFormat.getNumberInstance(locale).let { it as DecimalFormat }
    formatter.isGroupingUsed = true
    formatter.maximumIntegerDigits = integerPart.length
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0

    return formatter.format(BigInteger(integerPart))
}

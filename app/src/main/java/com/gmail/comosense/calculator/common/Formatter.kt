package com.gmail.comosense.calculator.common

import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

fun formatNumericString(value: String, locale: Locale): String {
    val pointIndex: Int = value.indexOf(".")
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

    val formattedIntegerPart: String = formatIntegerPart(
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

    val formatter: NumberFormat = NumberFormat.getNumberInstance(locale)
    if (formatter !is DecimalFormat) {
        return integerPart
    }

    formatter.isGroupingUsed = true
    formatter.maximumIntegerDigits = integerPart.length
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0

    return formatter.format(BigInteger(integerPart))
}

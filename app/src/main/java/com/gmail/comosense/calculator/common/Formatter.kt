package com.gmail.comosense.calculator.common

import com.gmail.comosense.calculator.presentation.Symbol

fun formatNumericString(value: String): String {
    val pointIndex: Int = value.indexOf(Symbol.Numeric.Point.text)

    val integerPart: String = if (pointIndex >= 0) {
        value.substring(0, pointIndex)
    } else {
        value
    }

    val decimalPart: String = if (pointIndex >= 0) {
        value.substring(pointIndex)
    } else {
        ""
    }

    val formattedIntegerPart = integerPart
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

    return formattedIntegerPart + decimalPart
}

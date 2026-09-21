package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit

@Immutable
data class CalculatorScreenDimensions(
    val expressionMaxFontSize: TextUnit,
    val expressionMinFontSize: TextUnit,
    val keyTextSize: TextUnit,
)

@Immutable
data class HistoryScreenDimensions(
    val expressionFontSize: TextUnit,
    val resultFontSize: TextUnit,
    val deleteAllFontSize: TextUnit,
)

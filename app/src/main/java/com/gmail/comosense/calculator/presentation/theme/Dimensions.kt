package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Immutable
data class CalculatorScreenDimensions(
    val expressionMaxFontSize: TextUnit,
    val expressionMinFontSize: TextUnit,
    val keyTextSize: TextUnit,
    val keyIconSize: Dp,
)

@Immutable
data class HistoryScreenDimensions(
    val expressionFontSize: TextUnit,
    val resultFontSize: TextUnit,
    val deleteAllFontSize: TextUnit,
)

package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

@Immutable
data class CalculatorScreenDimensions(
    val expressionMaxFontSize: TextUnit,
    val expressionMinFontSize: TextUnit,
    val keyTextFontSize: TextUnit,
    val keyIconSize: Dp,
)

@Immutable
data class HistoryScreenDimensions(
    val textFontSize: TextUnit,
    val expressionFontSize: TextUnit,
    val resultFontSize: TextUnit,
    val buttonFontSize: TextUnit,
)

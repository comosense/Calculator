package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CalculatorScreenColors(
    val background: Color,
    val expression: Color,
    val digitKeyContainer: Color,
    val digitKeyContent: Color,
    val operatorKeyContainer: Color,
    val operatorKeyContent: Color,
    val commandKeyContainer: Color,
    val commandKeyContent: Color,
    val uiKeyContainer: Color,
    val uiKeyContent: Color
)

@Immutable
data class HistoryScreenColors(
    val background: Color,
    val container: Color,
    val content: Color,
    val deleteContainer: Color,
    val deleteContent: Color,
    val deleteIcon: Color,
    val deleteAllContainer: Color,
    val deleteAllContent: Color,
)

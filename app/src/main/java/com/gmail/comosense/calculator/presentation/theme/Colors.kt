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
    val special: Color,
)

@Immutable
data class HistoryScreenColors(
    val background: Color,
    val textContent: Color,
    val historyContainer: Color,
    val historyContent: Color,
    val historyExpression: Color,
    val historyResult: Color,
    val deleteContainer: Color,
    val deleteContent: Color,
    val deleteIcon: Color,
    val deleteExpression: Color,
    val deleteResult: Color,
    val backButtonContainer: Color,
    val backButtonContent: Color,
    val deleteAllButtonContainer: Color,
    val deleteAllButtonContent: Color,
)

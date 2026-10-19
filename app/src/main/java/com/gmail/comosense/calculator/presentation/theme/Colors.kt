package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SettingsScreenColors(
    val background: Color,
    val label: Color,
    val picker: Color,
    val backButtonContainer: Color,
    val backButtonContent: Color,
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

@Immutable
data class CalculatorScreenColors(
    val background: Color,
    val expression: Color,
    val expressionBlinkBackground: Color,
    val digitKeyContainer: Color,
    val digitKeyContent: Color,
    val operatorKeyContainer: Color,
    val operatorKeyContent: Color,
    val commandKeyContainer: Color,
    val commandKeyContent: Color,
    val specialKeyContainer: Color,
    val specialKeyContent: Color,
    val keySwitcher: Color,
)

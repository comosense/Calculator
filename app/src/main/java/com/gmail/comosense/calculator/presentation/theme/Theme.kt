package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme

private val LocalSettingsScreenColors: ProvidableCompositionLocal<SettingsScreenColors> =
    staticCompositionLocalOf {
        error("SettingsScreenColors is not provided")
    }
private val LocalHistoryScreenColors: ProvidableCompositionLocal<HistoryScreenColors> =
    staticCompositionLocalOf {
        error("HistoryScreenColors is not provided")
    }
private val LocalCalculatorScreenColors: ProvidableCompositionLocal<CalculatorScreenColors> =
    staticCompositionLocalOf {
        error("CalculatorScreenColors is not provided")
    }
private val LocalSettingsScreenDimensions: ProvidableCompositionLocal<SettingsScreenDimensions> =
    staticCompositionLocalOf {
        error("SettingsScreenDimensions is not provided")
    }
private val LocalHistoryScreenDimensions: ProvidableCompositionLocal<HistoryScreenDimensions> =
    staticCompositionLocalOf {
        error("HistoryScreenDimensions is not provided")
    }
private val LocalCalculatorScreenDimensions: ProvidableCompositionLocal<CalculatorScreenDimensions> =
    staticCompositionLocalOf {
        error("CalculatorScreenDimensions is not provided")
    }

object AppTheme {
    val settingsScreenColors: SettingsScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsScreenColors.current
    val historyScreenColors: HistoryScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHistoryScreenColors.current
    val calculatorScreenColors: CalculatorScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCalculatorScreenColors.current
    val settingsScreenDimensions: SettingsScreenDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsScreenDimensions.current
    val historyScreenDimensions: HistoryScreenDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalHistoryScreenDimensions.current
    val calculatorScreenDimensions: CalculatorScreenDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalCalculatorScreenDimensions.current
}

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        val settingsScreenColors = SettingsScreenColors(
            background = MaterialTheme.colorScheme.background,
            label = MaterialTheme.colorScheme.onBackground,
            picker = MaterialTheme.colorScheme.onBackground,
            backButtonContainer = MaterialTheme.colorScheme.primaryContainer,
            backButtonContent = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        val historyScreenColors = HistoryScreenColors(
            background = MaterialTheme.colorScheme.background,
            textContent = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            historyContainer = MaterialTheme.colorScheme.surfaceContainer,
            historyContent = MaterialTheme.colorScheme.onSurface,
            historyExpression = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            historyResult = MaterialTheme.colorScheme.onSurface,
            deleteContainer = MaterialTheme.colorScheme.tertiaryContainer,
            deleteContent = MaterialTheme.colorScheme.onTertiaryContainer,
            deleteIcon = MaterialTheme.colorScheme.tertiaryDim,
            deleteExpression = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f),
            deleteResult = MaterialTheme.colorScheme.onTertiaryContainer,
            backButtonContainer = MaterialTheme.colorScheme.primaryContainer,
            backButtonContent = MaterialTheme.colorScheme.onPrimaryContainer,
            deleteAllButtonContainer = MaterialTheme.colorScheme.errorContainer,
            deleteAllButtonContent = MaterialTheme.colorScheme.onErrorContainer,
        )
        val calculatorScreenColors = CalculatorScreenColors(
            background = MaterialTheme.colorScheme.background,
            expression = MaterialTheme.colorScheme.onBackground,
            expressionBlinkBackground = MaterialTheme.colorScheme.primaryDim,
            digitKeyContainer = MaterialTheme.colorScheme.background,
            digitKeyContent = MaterialTheme.colorScheme.onBackground,
            operatorKeyContainer = MaterialTheme.colorScheme.surfaceContainerLow,
            operatorKeyContent = MaterialTheme.colorScheme.onSurface,
            commandKeyContainer = MaterialTheme.colorScheme.surfaceContainer,
            commandKeyContent = MaterialTheme.colorScheme.onSurface,
            keySwitcher = MaterialTheme.colorScheme.tertiary,
        )
        val settingsScreenDimensions = SettingsScreenDimensions(
            labelFontSize = 24.sp,
            pickerFontSize = 32.sp,
        )
        val historyScreenDimensions = HistoryScreenDimensions(
            textFontSize = 18.sp,
            expressionFontSize = 16.sp,
            resultFontSize = 18.sp,
            buttonFontSize = 18.sp,
        )
        val calculatorScreenDimensions = CalculatorScreenDimensions(
            expressionMaxFontSize = 32.sp,
            expressionMinFontSize = 16.sp,
            keyTextFontSize = 18.sp,
            keyIconSize = 20.dp,
        )
        CompositionLocalProvider(
            LocalSettingsScreenColors provides settingsScreenColors,
            LocalHistoryScreenColors provides historyScreenColors,
            LocalCalculatorScreenColors provides calculatorScreenColors,
            LocalSettingsScreenDimensions provides settingsScreenDimensions,
            LocalHistoryScreenDimensions provides historyScreenDimensions,
            LocalCalculatorScreenDimensions provides calculatorScreenDimensions,
        ) {
            content()
        }
    }
}

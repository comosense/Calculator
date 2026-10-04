package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme

private val LocalCalculatorScreenColors: ProvidableCompositionLocal<CalculatorScreenColors> =
    staticCompositionLocalOf {
        error("CalculatorScreenColors is not provided")
    }
private val LocalHistoryScreenColors: ProvidableCompositionLocal<HistoryScreenColors> =
    staticCompositionLocalOf {
        error("HistoryScreenColors is not provided")
    }
private val LocalCalculatorScreenDimensions: ProvidableCompositionLocal<CalculatorScreenDimensions> =
    staticCompositionLocalOf {
        error("CalculatorScreenDimensions is not provided")
    }
private val LocalHistoryScreenDimensions: ProvidableCompositionLocal<HistoryScreenDimensions> =
    staticCompositionLocalOf {
        error("HistoryScreenDimensions is not provided")
    }

object AppTheme {
    val calculatorScreenColors: CalculatorScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCalculatorScreenColors.current
    val historyScreenColors: HistoryScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHistoryScreenColors.current
    val calculatorScreenDimensions: CalculatorScreenDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalCalculatorScreenDimensions.current
    val historyScreenDimensions: HistoryScreenDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalHistoryScreenDimensions.current
}

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        val calculatorScreenColors = CalculatorScreenColors(
            background = MaterialTheme.colorScheme.surfaceContainer,
            expression = MaterialTheme.colorScheme.onSurface,
            digitKeyContainer = MaterialTheme.colorScheme.surfaceContainer,
            digitKeyContent = MaterialTheme.colorScheme.onSurface,
            operatorKeyContainer = MaterialTheme.colorScheme.tertiary,
            operatorKeyContent = MaterialTheme.colorScheme.onTertiary,
            commandKeyContainer = MaterialTheme.colorScheme.primary,
            commandKeyContent = MaterialTheme.colorScheme.onPrimary,
            special = MaterialTheme.colorScheme.tertiaryContainer,
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

        val calculatorScreenDimensions = CalculatorScreenDimensions(
            expressionMaxFontSize = 32.sp,
            expressionMinFontSize = 16.sp,
            keyTextFontSize = 18.sp,
            keyIconSize = 20.dp,
        )

        val historyScreenDimensions = HistoryScreenDimensions(
            textFontSize = 18.sp,
            expressionFontSize = 16.sp,
            resultFontSize = 18.sp,
            buttonFontSize = 18.sp,
        )

        CompositionLocalProvider(
            LocalCalculatorScreenColors provides calculatorScreenColors,
            LocalHistoryScreenColors provides historyScreenColors,
            LocalCalculatorScreenDimensions provides calculatorScreenDimensions,
            LocalHistoryScreenDimensions provides historyScreenDimensions,
        ) {
            content()
        }
    }
}

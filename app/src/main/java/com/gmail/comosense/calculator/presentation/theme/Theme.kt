package com.gmail.comosense.calculator.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.wear.compose.material3.MaterialTheme

private val LocalCalculatorScreenColors = staticCompositionLocalOf<CalculatorScreenColors> {
    error("CalculatorScreenColors is not provided")
}
private val LocalHistoryScreenColors = staticCompositionLocalOf<HistoryScreenColors> {
    error("HistoryScreenColors is not provided")
}

object CalculatorTheme {
    val calculatorScreenColors: CalculatorScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCalculatorScreenColors.current
    val historyScreenColors: HistoryScreenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHistoryScreenColors.current
}


@Composable
fun CalculatorTheme(
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
            uiKeyContainer = MaterialTheme.colorScheme.tertiary,
            uiKeyContent = MaterialTheme.colorScheme.onTertiary,
        )
        val historyScreenColors = HistoryScreenColors(
            container = MaterialTheme.colorScheme.background,
            content = MaterialTheme.colorScheme.onBackground,
            deleteContainer = MaterialTheme.colorScheme.tertiaryContainer,
            deleteContent = MaterialTheme.colorScheme.onTertiaryContainer,
            deleteIcon = MaterialTheme.colorScheme.tertiaryDim,
            deleteAllContainer = MaterialTheme.colorScheme.errorContainer,
            deleteAllContent = MaterialTheme.colorScheme.onErrorContainer,
        )

        CompositionLocalProvider(
            LocalCalculatorScreenColors provides calculatorScreenColors,
            LocalHistoryScreenColors provides historyScreenColors,
        ) {
            content()
        }
    }
}

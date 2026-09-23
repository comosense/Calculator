package com.gmail.comosense.calculator.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.SwipeToDismissBox
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WearApp(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    errorEvent: Flow<CalculatorServiceError>,
    locale: Locale,
) {
    var showHistory: Boolean by remember { mutableStateOf(false) }
    var isHistoryDeleteMode: Boolean by remember { mutableStateOf(false) }
    val swipeToDismissBoxState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()

    CalculatorServiceErrorToast(errorEvent)

    LaunchedEffect(swipeToDismissBoxState.currentValue) {
        if (swipeToDismissBoxState.currentValue == SwipeToDismissValue.Dismissed) {
            showHistory = false
            isHistoryDeleteMode = false
            swipeToDismissBoxState.snapTo(SwipeToDismissValue.Default)
        }
    }

    if (showHistory) {
        SwipeToDismissBox(
            state = swipeToDismissBoxState,
            userSwipeEnabled = !isHistoryDeleteMode,
            backgroundScrimColor = CalculatorTheme.calculatorScreenColors.background,
            backgroundKey = "CalculatorScreen",
            contentKey = "HistoryScreen",
        ) { isBackground ->
            if (isBackground) {
                CalculatorScreen(
                    appState = appState,
                    onAction = onAction,
                    onShowHistory = {},
                    locale = locale,
                )
            } else {
                HistoryScreen(
                    histories = appState.histories,
                    onAction = onAction,
                    onBack = {
                        coroutineScope.launch {
                            swipeToDismissBoxState.snapTo(SwipeToDismissValue.Dismissed)
                        }
                    },
                    onDeleteModeChanged = { isHistoryDeleteMode = it },
                    locale = locale,
                )
            }
        }
    } else {
        CalculatorScreen(
            appState = appState,
            onAction = onAction,
            onShowHistory = { showHistory = true },
            locale = locale,
        )
    }
}

@Composable
private fun CalculatorServiceErrorToast(errorEvent: Flow<CalculatorServiceError>) {
    val context: Context = LocalContext.current

    fun CalculatorServiceError.messageResourceId(): Int = when (this) {
        CalculatorServiceError.InvalidResult -> R.string.invalid_result
        CalculatorServiceError.InvalidExpression -> R.string.invalid_expression
        CalculatorServiceError.DivisionByZero -> R.string.division_by_zero
        CalculatorServiceError.Arithmetic -> R.string.arithmetic
    }

    LaunchedEffect(errorEvent) {
        errorEvent.collect { error ->
            Toast.makeText(
                context,
                error.messageResourceId(),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

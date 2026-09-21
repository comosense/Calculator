package com.gmail.comosense.calculator.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gmail.comosense.calculator.R
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Composable
fun WearApp(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    errorEvent: Flow<CalculatorServiceError>,
    locale: Locale,
) {
    var showHistory: Boolean by remember { mutableStateOf(false) }

    CalculatorServiceErrorToast(errorEvent)

    AnimatedContent(
        targetState = showHistory,
        transitionSpec = {
            if (targetState) {
                slideInVertically(
                    initialOffsetY = { -it }
                ) togetherWith slideOutVertically(
                    targetOffsetY = { it }
                )
            } else {
                slideInVertically(
                    initialOffsetY = { it }
                ) togetherWith slideOutVertically(
                    targetOffsetY = { -it }
                )
            }
        },
    ) { isShowHistory ->
        if (isShowHistory) {
            HistoryScreen(
                history = appState.history,
                onAction = onAction,
                onBack = { showHistory = false },
                locale = locale,
            )
        } else {
            CalculatorScreen(
                appState = appState,
                onAction = onAction,
                onShowHistory = { showHistory = true },
                locale = locale,
            )
        }
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

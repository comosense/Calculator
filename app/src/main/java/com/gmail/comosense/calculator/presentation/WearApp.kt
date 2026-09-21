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
import androidx.compose.ui.res.stringResource
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
    val messageInvalidResult: String = stringResource(R.string.invalid_result)
    val messageInvalidExpression: String = stringResource(R.string.invalid_expression)
    val messageDivisionByZero: String = stringResource(R.string.division_by_zero)
    val messageArithmetic: String = stringResource(R.string.arithmetic)

    LaunchedEffect(errorEvent) {
        errorEvent.collect { error ->
            val message: String = when (error) {
                CalculatorServiceError.InvalidResult -> messageInvalidResult
                CalculatorServiceError.InvalidExpression -> messageInvalidExpression
                CalculatorServiceError.DivisionByZero -> messageDivisionByZero
                CalculatorServiceError.Arithmetic -> messageArithmetic
            }
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

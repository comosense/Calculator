package com.gmail.comosense.calculator.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.SwipeToDismissBox
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.data.HistoryRepositoryError
import com.gmail.comosense.calculator.domain.CalculatorError
import com.gmail.comosense.calculator.presentation.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun WearApp(
    appState: AppState,
    appEvent: Flow<AppEvent>,
    onAction: (AppAction) -> Unit,
    locale: Locale,
) {
    var showHistory: Boolean by remember { mutableStateOf(false) }
    var isHistoryDeleteMode: Boolean by remember { mutableStateOf(false) }
    val swipeToDismissBoxState: SwipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val symbolFormatter: SymbolFormatter = remember(locale) { SymbolFormatter(locale) }

    HandleEvent(appEvent)

    LaunchedEffect(swipeToDismissBoxState.currentValue) {
        if (swipeToDismissBoxState.currentValue == SwipeToDismissValue.Dismissed) {
            showHistory = false
            isHistoryDeleteMode = false
        }
    }

    AppScaffold {
        CalculatorScreen(
            appState = appState,
            onAction = onAction,
            onShowHistory = {
                coroutineScope.launch {
                    swipeToDismissBoxState.snapTo(SwipeToDismissValue.Default)
                    showHistory = true
                }
            },
            symbolFormatter = symbolFormatter,
        )

        AnimatedVisibility(
            visible = showHistory,
            enter = fadeIn(
                animationSpec = tween(180),
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(180),
            ),
            exit = ExitTransition.None,
            modifier = Modifier.fillMaxSize(),
        ) {
            SwipeToDismissBox(
                state = swipeToDismissBoxState,
                userSwipeEnabled = !isHistoryDeleteMode,
                backgroundScrimColor = AppTheme.calculatorScreenColors.background,
                backgroundKey = "CalculatorScreen",
                contentKey = "HistoryScreen",
            ) { isBackground ->
                if (isBackground) {
                    CalculatorScreen(
                        appState = appState,
                        onAction = onAction,
                        onShowHistory = {},
                        symbolFormatter = symbolFormatter,
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
                        symbolFormatter = symbolFormatter,
                    )
                }
            }
        }
    }
}

@Composable
private fun HandleEvent(appEvent: Flow<AppEvent>) {
    val context: Context = LocalContext.current

    LaunchedEffect(appEvent) {
        appEvent.collect { appEvent ->
            val messageResourceId: Int = when (appEvent) {
                is AppEvent.CalculatorError -> {
                    appEvent.calculatorError.messageResourceId()
                }

                is AppEvent.HistoryRepositoryError -> {
                    appEvent.historyRepositoryError.messageResourceId()
                }
            }

            Toast.makeText(
                context,
                messageResourceId,
                Toast.LENGTH_SHORT
            ).show()

        }
    }
}

private fun CalculatorError.messageResourceId(): Int = when (this) {
    CalculatorError.InvalidExpression -> R.string.calculator_error_invalid_expression
    CalculatorError.DivisionByZero -> R.string.calculator_error_division_by_zero
    CalculatorError.FactorialInvalidArgument -> R.string.calculator_error_factorial_invalid_argument
    CalculatorError.FactorialLargeArgument -> R.string.calculator_error_factorial_large_argument
    CalculatorError.SqrtInvalidArgument -> R.string.calculator_error_sqrt_invalid_argument
    CalculatorError.PowInvalidArgument -> R.string.calculator_error_pow_invalid_argument
    CalculatorError.PowLimitExceeded -> R.string.calculator_error_pow_limit_exceeded
    CalculatorError.LogInvalidArgument -> R.string.calculator_error_log_invalid_argument
    CalculatorError.TanInvalidArgument -> R.string.calculator_error_tan_invalid_argument
    CalculatorError.Arithmetic -> R.string.calculator_error_arithmetic
    CalculatorError.Unsupported -> R.string.calculator_error_unsupported
}

private fun HistoryRepositoryError.messageResourceId(): Int = when (this) {
    HistoryRepositoryError.Add -> R.string.history_repository_error_add
    HistoryRepositoryError.Delete -> R.string.history_repository_error_delete
    HistoryRepositoryError.DeleteAll -> R.string.history_repository_error_delete_all
    HistoryRepositoryError.TooLarge -> R.string.history_repository_error_too_large
    HistoryRepositoryError.InvalidHistory -> R.string.history_repository_error_invalid_history
}

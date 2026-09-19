package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun WearApp(
    appState: AppState,
    onAction: (AppAction) -> Unit,
) {
    var showHistory: Boolean by remember { mutableStateOf(false) }

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
            )
        } else {
            CalculatorScreen(
                appState = appState,
                onAction = onAction,
                onShowHistory = { showHistory = true },
            )
        }
    }
}

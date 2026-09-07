package com.gmail.comosense.calculator.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun WearApp(
    appState: AppState,
    onClick: (Key) -> Unit,
    onLongClick: (Key?) -> Unit,
    onHistoryClick: (List<Symbol>) -> Unit,
    onHistoryDelete: (Int) -> Unit,
    onHistoryDeleteAll: () -> Unit,
) {
    var showHistory: Boolean by remember { mutableStateOf(false) }

    if (showHistory) {
        HistoryScreen(
            history = appState.history,
            onHistoryClick = onHistoryClick,
            onHistoryDelete = onHistoryDelete,
            onHistoryDeleteAll = onHistoryDeleteAll,
            onBack = { showHistory = false },
        )
    } else {
        CalculatorScreen(
            appState = appState,
            onClick = onClick,
            onLongClick = onLongClick,
            onShowHistory = { showHistory = true },
        )
    }
}

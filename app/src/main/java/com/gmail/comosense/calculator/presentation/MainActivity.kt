package com.gmail.comosense.calculator.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appState: AppState by viewModel.appState
            WearApp(
                appState = appState,
                onClick = viewModel::onClick,
                onLongClick = viewModel::onLongClick,
                onHistoryClick = viewModel::onHistoryClick,
                onHistoryDelete = viewModel::onHistoryDelete,
                onHistoryDeleteAll = viewModel::onHistoryDeleteAll,
            )
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp(
        appState = AppState(
            entering = listOf(Symbol.Error("01234567890123456789")),
        ),
        onClick = {},
        onLongClick = {},
        onHistoryClick = {},
        onHistoryDelete = {},
        onHistoryDeleteAll = {},
    )
}

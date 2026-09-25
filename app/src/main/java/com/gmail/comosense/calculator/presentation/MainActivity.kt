package com.gmail.comosense.calculator.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLocale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import kotlinx.coroutines.flow.emptyFlow

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        setContent {
            CalculatorTheme {
                val appState: AppState by viewModel.appState.collectAsStateWithLifecycle()

                WearApp(
                    appState = appState,
                    onAction = viewModel::onAction,
                    errorEvent = viewModel.errorEvent,
                    locale = LocalLocale.current.platformLocale,
                )
            }
        }
    }
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    CalculatorTheme {
        WearApp(
            appState = AppState(
                entering = listOf(
                    Symbol.Numeric.Digit(1),
                    Symbol.Operator.Add,
                    Symbol.Numeric.Digit(2),
                ),
            ),
            onAction = {},
            errorEvent = emptyFlow(),
            locale = LocalLocale.current.platformLocale,
        )
    }
}

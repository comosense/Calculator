package com.gmail.comosense.calculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.data.Settings
import com.gmail.comosense.calculator.presentation.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SettingsScreen(
    settings: Settings,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold {
        val precisionOptions: List<Int> =
            listOf(8, 16, 24, 32, 40, 48, 56, 64)
        val precisionIndex: Int =
            precisionOptions.indexOf(settings.precision).coerceAtLeast(0)
        val precisionPickerState =
            rememberPickerState(
                initialNumberOfOptions = precisionOptions.size,
                initiallySelectedIndex = precisionIndex,
                shouldRepeatOptions = false,
            )

        LaunchedEffect(precisionPickerState) {
            snapshotFlow { precisionPickerState.selectedOptionIndex }
                .distinctUntilChanged()
                .collect { index ->
                    onAction(
                        AppAction.SetPrecision(
                            precisionOptions[index]
                        )
                    )
                }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.settingsScreenColors.background),
        ) {
            val inscribedLengthRatio = 0.70710677f
            val displayWidth: Dp = maxWidth
            val displayHeight: Dp = maxHeight
            val inscribedLength: Dp = minOf(displayWidth, displayHeight) * inscribedLengthRatio

            val headerHeight: Dp = (displayHeight - inscribedLength) / 2f
            val buttonHeight: Dp = headerHeight * 1.5f
            val bodyHeight: Dp = displayHeight - (headerHeight + buttonHeight)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier.size(
                        width = displayWidth,
                        height = headerHeight,
                    )
                )

                Column(
                    modifier = Modifier.size(
                        width = inscribedLength,
                        height = bodyHeight,
                    ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_screen_label_precision),
                            color = AppTheme.settingsScreenColors.label,
                            fontSize = AppTheme.settingsScreenDimensions.labelFontSize,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Picker(
                        state = precisionPickerState,
                        contentDescription = { "${precisionPickerState.selectedOptionIndex + 1}" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(4f),
                    ) {
                        Text(
                            text = precisionOptions[it].toString(),
                            color = AppTheme.settingsScreenColors.picker,
                            fontSize = AppTheme.settingsScreenDimensions.pickerFontSize,
                        )
                    }
                }

                EdgeButton(
                    onClick = onBack,
                    modifier = Modifier.size(
                        width = displayWidth,
                        height = buttonHeight,
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.settingsScreenColors.backButtonContainer,
                        contentColor = AppTheme.settingsScreenColors.backButtonContent,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_undo),
                        contentDescription = stringResource(R.string.content_description_undo),
                        tint = AppTheme.settingsScreenColors.backButtonContent,
                    )
                }
            }
        }
    }
}

package com.gmail.comosense.calculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.data.Settings
import com.gmail.comosense.calculator.data.SettingsDataStoreConstraints
import com.gmail.comosense.calculator.presentation.theme.AppTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

private const val INSCRIBED_LENGTH_RATIO: Float = 0.70710677f
private const val DEFAULT_PRECISION = SettingsDataStoreConstraints.DEFAULT_PRECISION
private val PRECISION_OPTIONS: List<Int> = listOf(8, 16, 24, 32, 40, 48, 56, 64)

@Composable
fun SettingsScreen(
    settings: Settings,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        timeText = {},
    ) {
        val precisionIndex: Int = PRECISION_OPTIONS
            .indexOf(settings.precision)
            .let { if (it >= 0) it else PRECISION_OPTIONS.indexOf(DEFAULT_PRECISION) }
        val precisionPickerState =
            rememberPickerState(
                initialNumberOfOptions = PRECISION_OPTIONS.size,
                initiallySelectedIndex = precisionIndex,
                shouldRepeatOptions = false,
            )

        LaunchedEffect(precisionPickerState) {
            snapshotFlow { precisionPickerState.selectedOptionIndex }
                .map { PRECISION_OPTIONS[it] }
                .distinctUntilChanged()
                .drop(1)
                .collect { precision ->
                    onAction(AppAction.SetPrecision(precision))
                }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.settingsScreenColors.background),
        ) {
            val displayWidth: Dp = maxWidth
            val displayHeight: Dp = maxHeight

            val inscribedLength: Dp =
                minOf(displayWidth, displayHeight) * INSCRIBED_LENGTH_RATIO
            val topOrBottomMargin: Dp =
                (displayHeight - inscribedLength) / 2f

            val labelHeight: Dp = topOrBottomMargin
            val buttonHeight: Dp = topOrBottomMargin * 1.5f
            val pickerHeight: Dp =
                (displayHeight - (labelHeight + buttonHeight))
                    .coerceAtLeast(0.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(
                        width = displayWidth,
                        height = labelHeight,
                    ),
                    contentAlignment = Alignment.BottomCenter,
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
                    contentDescription = {
                        PRECISION_OPTIONS[precisionPickerState.selectedOptionIndex].toString()
                    },
                    modifier = Modifier.size(
                        width = inscribedLength,
                        height = pickerHeight,
                    ),
                ) {
                    Text(
                        text = PRECISION_OPTIONS[it].toString(),
                        color = AppTheme.settingsScreenColors.picker,
                        fontSize = AppTheme.settingsScreenDimensions.pickerFontSize,
                    )
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

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
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.data.Settings
import com.gmail.comosense.calculator.data.SettingsConstraints
import com.gmail.comosense.calculator.presentation.theme.AppTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

private const val INSCRIBED_LENGTH_RATIO: Float = 0.70710677f

@OptIn(FlowPreview::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onAppAction: (AppAction) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(timeText = {}) {
        val precisionIndex: Int = SettingsConstraints.VALID_PRECISIONS
            .indexOf(settings.precision)
            .let { index ->
                if (index >= 0) {
                    index
                } else {
                    SettingsConstraints.VALID_PRECISIONS
                        .indexOf(SettingsConstraints.DEFAULT_PRECISION)
                }
            }
        val precisionPickerState: PickerState =
            rememberPickerState(
                initialNumberOfOptions = SettingsConstraints.VALID_PRECISIONS.size,
                initiallySelectedIndex = precisionIndex,
                shouldRepeatOptions = false,
            )

        LaunchedEffect(precisionPickerState) {
            snapshotFlow { precisionPickerState.selectedOptionIndex }
                .map { index -> SettingsConstraints.VALID_PRECISIONS[index] }
                .distinctUntilChanged()
                .drop(1)
                .debounce(500.milliseconds)
                .collect { precision ->
                    onAppAction(AppAction.SetPrecision(precision))
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
            val subLabelHeight: Dp = labelHeight
            val buttonHeight: Dp = topOrBottomMargin * 1.5f
            val pickerHeight: Dp =
                (displayHeight - (labelHeight + subLabelHeight + buttonHeight))
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

                Box(
                    modifier = Modifier.size(
                        width = displayWidth,
                        height = subLabelHeight,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.settings_screen_sub_label_precision),
                        color = AppTheme.settingsScreenColors.subLabel,
                        fontSize = AppTheme.settingsScreenDimensions.subLabelFontSize,
                        textAlign = TextAlign.Center,
                        lineHeight = AppTheme.settingsScreenDimensions.subLabelFontSize,
                    )
                }

                Picker(
                    state = precisionPickerState,
                    contentDescription = {
                        SettingsConstraints.VALID_PRECISIONS[
                            precisionPickerState.selectedOptionIndex
                        ].toString()
                    },
                    modifier = Modifier.size(
                        width = inscribedLength,
                        height = pickerHeight,
                    ),
                ) { index ->
                    Text(
                        text = SettingsConstraints.VALID_PRECISIONS[index].toString(),
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

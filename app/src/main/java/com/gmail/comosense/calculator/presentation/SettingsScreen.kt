package com.gmail.comosense.calculator.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.PickerState
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import com.gmail.comosense.calculator.data.Settings
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class SettingsItem {
    CalculationPrecision,
    DisplayScale,
}

private val calculationPrecisionOptions: List<Int> = listOf(16, 24, 32, 48, 64)
private val displayScaleOptions: List<Int> = listOf(8, 12, 16, 20, 24, 32, 40, 48)

@Composable
fun SettingsScreen(
    settings: Settings,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
) {
    val initialItem: SettingsItem = SettingsItem.CalculationPrecision
    var selectedItem: SettingsItem by remember { mutableStateOf(initialItem) }

    val calculationPrecisionIndex: Int = calculationPrecisionOptions.indexOf(
        settings.calculationPrecision
    ).coerceAtLeast(0)
    val displayScaleIndex: Int = displayScaleOptions.indexOf(
        settings.displayScale
    ).coerceAtLeast(0)

    val calculationPrecisionPicker: PickerState = rememberPickerState(
        initialNumberOfOptions = calculationPrecisionOptions.size,
        initiallySelectedIndex = calculationPrecisionIndex,
        shouldRepeatOptions = false,
    )
    val displayScalePicker: PickerState = rememberPickerState(
        initialNumberOfOptions = displayScaleOptions.size,
        initiallySelectedIndex = displayScaleIndex,
        shouldRepeatOptions = false,
    )

    val activePicker: PickerState = when (selectedItem) {
        SettingsItem.CalculationPrecision -> calculationPrecisionPicker
        SettingsItem.DisplayScale -> displayScalePicker
    }
    val options: List<Int> = when (selectedItem) {
        SettingsItem.CalculationPrecision -> calculationPrecisionOptions
        SettingsItem.DisplayScale -> displayScaleOptions
    }
    val selectedValue by remember(selectedItem, activePicker) {
        derivedStateOf {
            options[activePicker.selectedOptionIndex]
        }
    }

    LaunchedEffect(calculationPrecisionPicker) {
        snapshotFlow { calculationPrecisionPicker.selectedOptionIndex }
            .distinctUntilChanged()
            .collect { index ->
                onAction(
                    AppAction.SetCalculationPrecision(
                        calculationPrecisionOptions[index]
                    )
                )
            }
    }

    LaunchedEffect(displayScalePicker) {
        snapshotFlow { displayScalePicker.selectedOptionIndex }
            .distinctUntilChanged()
            .collect { index ->
                onAction(
                    AppAction.SetDisplayScale(
                        displayScaleOptions[index]
                    )
                )
            }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = "TEMP_Settings"
        )

        Button(
            onClick = {
                selectedItem = when (selectedItem) {
                    SettingsItem.CalculationPrecision ->
                        SettingsItem.DisplayScale

                    SettingsItem.DisplayScale ->
                        SettingsItem.CalculationPrecision
                }
            },
        ) {
            Text(
                text = when (selectedItem) {
                    SettingsItem.CalculationPrecision ->
                        "TEMP_Precision"

                    SettingsItem.DisplayScale ->
                        "TEMP_DISPLAY digits"
                }
            )
        }

        Text(
            text = when (selectedItem) {
                SettingsItem.CalculationPrecision ->
                    "TEMP_Calculation precision"

                SettingsItem.DisplayScale ->
                    "TEMP_Digits after decimal"
            }
        )

        Box(
            modifier = Modifier.size(width = 100.dp, height = 110.dp),
            contentAlignment = Alignment.Center,
        ) {
            Picker(
                state = activePicker,
                contentDescription = { "$selectedValue" },
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                Text(
                    text = options[index].toString()
                )
            }

            Text(
                text = "TEMP_Selected: $selectedValue",
            )

            Button(onClick = onBack) {
                Text("TEMP_Back")
            }
        }
    }
}


package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.gmail.comosense.calculator.common.Constraints

internal object SettingsDataStoreConstraints {
    const val DEFAULT_CALCULATION_PRECISION: Int =
        Constraints.DEFAULT_CALCULATION_PRECISION
    const val DEFAULT_DISPLAY_SCALE: Int =
        Constraints.DEFAULT_DISPLAY_SCALE
}

val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class Settings(
    val calculationPrecision: Int =
        SettingsDataStoreConstraints.DEFAULT_CALCULATION_PRECISION,
    val displayScale: Int =
        SettingsDataStoreConstraints.DEFAULT_DISPLAY_SCALE,
)

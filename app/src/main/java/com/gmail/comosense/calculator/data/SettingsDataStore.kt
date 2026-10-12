package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class Settings(
    val calculationPrecision: Int = 32,
    val displayScale: Int = 24,
)

private object SettingsKeys {
    val CALCULATION_PRECISION: Preferences.Key<Int> =
        intPreferencesKey("calculation_precision")
    val DISPLAY_SCALE: Preferences.Key<Int> =
        intPreferencesKey("display_scale")
}

class SettingsRepository(
    private val context: Context,
) {
    companion object {
        val CALCULATION_PRECISION_OPTIONS: List<Int> =
            listOf(16, 24, 32, 48, 64)
        val DISPLAY_SCALE_OPTIONS: List<Int> =
            listOf(8, 12, 16, 20, 24, 32, 40, 48)
    }

    val settings: Flow<Settings> =
        context.settingsDataStore.data.map { preferences ->
            Settings(
                calculationPrecision =
                    preferences[SettingsKeys.CALCULATION_PRECISION]
                        ?: 32,
                displayScale =
                    preferences[SettingsKeys.DISPLAY_SCALE]
                        ?: 24,
            )
        }

    suspend fun updateCalculationPrecision(value: Int) {
        require(value in CALCULATION_PRECISION_OPTIONS)

        context.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.CALCULATION_PRECISION] = value
        }
    }

    suspend fun updateDisplayScale(value: Int) {
        require(value in DISPLAY_SCALE_OPTIONS)

        context.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.DISPLAY_SCALE] = value
        }
    }
}


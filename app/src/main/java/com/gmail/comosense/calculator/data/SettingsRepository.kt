package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    private object SettingsKeys {
        val CALCULATION_PRECISION: Preferences.Key<Int> =
            intPreferencesKey("calculation_precision")
    }

    val settings: Flow<Settings> =
        context.settingsDataStore.data.map { preferences ->
            Settings(
                calculationPrecision =
                    preferences[SettingsKeys.CALCULATION_PRECISION]
                        ?: SettingsDataStoreConstraints.DEFAULT_CALCULATION_PRECISION,
            )
        }

    suspend fun updateCalculationPrecision(value: Int) {
        require(value > 0)

        context.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.CALCULATION_PRECISION] = value
        }
    }
}

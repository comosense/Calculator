package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SettingsRepositoryError {
    UpdatePrecision,
}

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object SettingsKeys {
        val PRECISION: Preferences.Key<Int> =
            intPreferencesKey("precision")
    }

    private fun isValidPrecision(precision: Int): Boolean =
        precision in SettingsDataStoreConstraints.VALID_PRECISION_RANGE

    val settings: Flow<Settings> =
        dataStore.data.map { preferences ->
            val precision: Int = preferences[SettingsKeys.PRECISION]
                ?.takeIf(::isValidPrecision)
                ?: SettingsDataStoreConstraints.DEFAULT_PRECISION

            Settings(
                precision = precision
            )
        }

    suspend fun repairInvalidPrecision() {
        dataStore.edit { preferences ->
            val precision: Int? = preferences[SettingsKeys.PRECISION]
            if (precision != null && !isValidPrecision(precision)) {
                preferences[SettingsKeys.PRECISION] =
                    SettingsDataStoreConstraints.DEFAULT_PRECISION
            }
        }
    }

    suspend fun updatePrecision(
        precision: Int,
    ): Result<Unit, SettingsRepositoryError> {
        if (!isValidPrecision(precision)) {
            return Result.Err(SettingsRepositoryError.UpdatePrecision)
        }

        try {
            dataStore.edit { preferences ->
                preferences[SettingsKeys.PRECISION] = precision
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(SettingsRepositoryError.UpdatePrecision)
        }

        return Result.Ok(Unit)
    }
}

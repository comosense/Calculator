package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SettingsRepositoryError {
    RepairInvalidSettings,
    UpdatePrecision,
}

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private object SettingsKeys {
        val PRECISION: Preferences.Key<Int> =
            intPreferencesKey("precision")
    }

    val settings: Flow<Settings> =
        dataStore.data.map { preferences ->
            val precision: Int = preferences[SettingsKeys.PRECISION]
                ?.takeIf(::isValidPrecision)
                ?: SettingsConstraints.DEFAULT_PRECISION

            Settings(
                precision = precision
            )
        }

    suspend fun repairInvalidSettings(): Result<Unit, SettingsRepositoryError> {
        return edit(
            error = SettingsRepositoryError.RepairInvalidSettings,
            transform = { preferences ->
                val precision: Int? = preferences[SettingsKeys.PRECISION]
                if (precision != null && !isValidPrecision(precision)) {
                    preferences[SettingsKeys.PRECISION] =
                        SettingsConstraints.DEFAULT_PRECISION
                }
            },
        )
    }

    suspend fun updatePrecision(
        precision: Int,
    ): Result<Unit, SettingsRepositoryError> {
        if (!isValidPrecision(precision)) {
            return Result.Err(SettingsRepositoryError.UpdatePrecision)
        }

        return edit(
            error = SettingsRepositoryError.UpdatePrecision,
            transform = { preferences ->
                preferences[SettingsKeys.PRECISION] = precision
            },
        )
    }

    private fun isValidPrecision(precision: Int): Boolean =
        precision in SettingsConstraints.VALID_PRECISIONS

    private suspend fun edit(
        error: SettingsRepositoryError,
        transform: suspend (MutablePreferences) -> Unit,
    ): Result<Unit, SettingsRepositoryError> {
        try {
            dataStore.edit(transform)
            return Result.Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(error)
        }
    }
}

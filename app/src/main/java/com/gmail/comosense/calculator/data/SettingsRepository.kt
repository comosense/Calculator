package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SettingsRepositoryError {
    UpdatePrecision
}

class SettingsRepository(private val context: Context) {
    private object SettingsKeys {
        val PRECISION: Preferences.Key<Int> =
            intPreferencesKey("precision")
    }

    val settings: Flow<Settings> =
        context.settingsDataStore.data.map { preferences ->
            Settings(
                precision =
                    preferences[SettingsKeys.PRECISION]
                        ?: SettingsDataStoreConstraints.DEFAULT_PRECISION,
            )
        }

    suspend fun updatePrecision(value: Int): Result<Unit, SettingsRepositoryError> {
        require(value > 0)

        try {
            context.settingsDataStore.edit { preferences ->
                preferences[SettingsKeys.PRECISION] = value
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(SettingsRepositoryError.UpdatePrecision)
        }

        return Result.Ok(Unit)
    }
}

package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler {
        emptyPreferences()
    },
)

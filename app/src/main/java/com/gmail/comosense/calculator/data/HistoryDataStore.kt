package com.gmail.comosense.calculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.gmail.comosense.calculator.data.proto.HistoryStore

val Context.historyDataStore: DataStore<HistoryStore> by dataStore(
    fileName = "history.pb",
    serializer = HistorySerializer,
)

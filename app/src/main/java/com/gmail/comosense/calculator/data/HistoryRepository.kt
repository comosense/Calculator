package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import com.gmail.comosense.calculator.data.proto.HistoryStore
import com.gmail.comosense.calculator.domain.Calculation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HistoryRepository(private val dataStore: DataStore<HistoryStore>) {
    companion object {
        private const val HISTORY_SIZE: Int = 50
    }

    val history: Flow<List<History>> =
        dataStore.data.map { store ->
            store.historiesList.mapNotNull { calculation ->
                calculation.toHistoryOrNull()
            }
        }

    suspend fun addHistory(calculation: Calculation) {
        dataStore.updateData { store ->
            val newHistory = History(
                id = UUID.randomUUID().toString(),
                calculation = calculation,
            )

            store.toBuilder()
                .clearHistories()
                .addHistories(newHistory.toProto())
                .addAllHistories(store.historiesList.take(HISTORY_SIZE - 1))
                .build()
        }
    }

    suspend fun deleteHistory(id: String) {
        dataStore.updateData { store ->
            val index = store.historiesList
                .indexOfFirst { it.id == id }

            if (index < 0) {
                store
            } else {
                store.toBuilder()
                    .removeHistories(index)
                    .build()
            }
        }
    }

    suspend fun deleteHistoryAll() {
        dataStore.updateData { store ->
            store.toBuilder()
                .clearHistories()
                .build()
        }
    }
}

package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import com.gmail.comosense.calculator.data.proto.HistoryStore
import com.gmail.comosense.calculator.domain.Symbol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HistoryRepository(private val dataStore: DataStore<HistoryStore>) {
    companion object {
        private const val HISTORIES_SIZE: Int = 50
    }

    val history: Flow<List<History>> =
        dataStore.data.map { store ->
            store.historiesList.mapNotNull { history ->
                history.toHistoryOrNull()
            }
        }

    suspend fun addHistory(expression: List<Symbol>, result: List<Symbol>) {
        dataStore.updateData { store ->
            val newHistory = History(
                id = UUID.randomUUID().toString(),
                expression = expression,
                result = result,
            )

            val builder: HistoryStore.Builder = store
                .toBuilder()
                .addHistories(0, newHistory.toProto())
            while (builder.historiesCount > HISTORIES_SIZE) {
                builder.removeHistories(builder.historiesCount - 1)
            }
            builder.build()
        }
    }

    suspend fun deleteHistory(id: String) {
        dataStore.updateData { store ->
            val index: Int = store.historiesList
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

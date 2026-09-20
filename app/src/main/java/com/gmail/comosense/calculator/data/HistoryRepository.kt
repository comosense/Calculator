package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import com.gmail.comosense.calculator.data.proto.HistoryStore
import com.gmail.comosense.calculator.domain.Calculation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dataStore: DataStore<HistoryStore>) {
    companion object {
        private const val HISTORY_SIZE: Int = 50
    }

    val history: Flow<List<Calculation>> =
        dataStore.data.map { store ->
            store.calculationsList.map { it.toCalculation() }
        }

    suspend fun addHistory(calculation: Calculation) {
        dataStore.updateData { store ->
            store.toBuilder()
                .clearCalculations()
                .addCalculations(calculation.toProto())
                .addAllCalculations(store.calculationsList.take(HISTORY_SIZE - 1))
                .build()
        }
    }

    suspend fun deleteHistory(index: Int) {
        dataStore.updateData { store ->
            if (index !in store.calculationsList.indices) {
                store
            } else {
                store.toBuilder()
                    .removeCalculations(index)
                    .build()
            }
        }
    }

    suspend fun deleteHistoryAll() {
        dataStore.updateData { store ->
            store.toBuilder()
                .clearCalculations()
                .build()
        }
    }
}

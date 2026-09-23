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

    val history: Flow<List<Calculation>> =
        dataStore.data.map { store ->
            store.calculationsList.mapNotNull { calculation ->
                calculation.toCalculationOrNull()
            }
        }

    suspend fun addHistory(calculation: Calculation) {
        dataStore.updateData { store ->
            val newCalculation = calculation
                .copy(id = UUID.randomUUID().toString())
                .toProto()

            store.toBuilder()
                .clearCalculations()
                .addCalculations(newCalculation)
                .addAllCalculations(store.calculationsList.take(HISTORY_SIZE - 1))
                .build()
        }
    }

    suspend fun deleteHistory(id: String) {
        dataStore.updateData { store ->
            val index = store.calculationsList
                .indexOfFirst { it.id == id }

            if (index < 0) {
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

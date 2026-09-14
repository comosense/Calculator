package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import com.gmail.comosense.calculator.data.proto.HistoryStore
import com.gmail.comosense.calculator.presentation.Calculation
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
                .addAllCalculations(
                    (listOf(calculation) + store.calculationsList.map { it.toCalculation() })
                        .take(HISTORY_SIZE)
                        .map { it.toProto() })
                .build()
        }
    }

    suspend fun deleteHistory(index: Int) {
        dataStore.updateData { store ->
            if (index !in store.calculationsList.indices) {
                return@updateData store
            }

            store.toBuilder()
                .clearCalculations()
                .addAllCalculations(store.calculationsList.filterIndexed { i, _ -> i != index })
                .build()
        }
    }

    suspend fun deleteAllHistory() {
        dataStore.updateData { store ->
            store.toBuilder()
                .clearCalculations()
                .build()
        }
    }
}

package com.gmail.comosense.calculator.data

import android.content.Context
import com.gmail.comosense.calculator.presentation.Calculation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val context: Context) {
    companion object {
        private const val HISTORY_SIZE = 50
    }

    val history: Flow<List<Calculation>> =
        context.historyDataStore.data.map { store ->
            store.calculationsList.map { it.toCalculation() }
        }

    suspend fun addHistory(calculation: Calculation) {
        context.historyDataStore.updateData { store ->
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
        context.historyDataStore.updateData { store ->
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
        context.historyDataStore.updateData { store ->
            store.toBuilder()
                .clearCalculations()
                .build()
        }
    }
}

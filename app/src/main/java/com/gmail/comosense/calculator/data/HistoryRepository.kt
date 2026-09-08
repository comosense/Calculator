package com.gmail.comosense.calculator.data

import android.content.Context
import com.gmail.comosense.calculator.presentation.Calculation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(private val context: Context) {
    val history: Flow<List<Calculation>> =
        context.historyDataStore.data.map { store ->
            store.calculationsList.map { it.toCalculation() }
        }

    suspend fun saveHistory(history: List<Calculation>) {
        context.historyDataStore.updateData { store ->
            store.toBuilder()
                .clearCalculations()
                .addAllCalculations(history.map { it.toProto() })
                .build()
        }
    }
}

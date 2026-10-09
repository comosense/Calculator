package com.gmail.comosense.calculator.data

import androidx.datastore.core.DataStore
import com.gmail.comosense.calculator.common.Result
import com.gmail.comosense.calculator.data.proto.HistoryStore
import com.gmail.comosense.calculator.domain.Symbol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

enum class HistoryRepositoryError {
    Add,
    Delete,
    DeleteAll,
    TooLarge,
}

class HistoryRepository(private val dataStore: DataStore<HistoryStore>) {
    val histories: Flow<List<History>> =
        dataStore.data.map { store ->
            store.historiesList
                .mapNotNull { it.toHistoryOrNull() }
                .take(HistoryConstraints.MAX_HISTORIES_SIZE)
        }

    suspend fun addHistory(
        expression: List<Symbol>,
        result: List<Symbol>
    ): Result<Unit, HistoryRepositoryError> {
        if (expression.size > HistoryConstraints.MAX_EXPRESSION_SIZE ||
            result.size > HistoryConstraints.MAX_RESULT_SIZE
        ) {
            return Result.Err(HistoryRepositoryError.TooLarge)
        }

        try {
            dataStore.updateData { store ->
                val newHistory = History(
                    id = UUID.randomUUID().toString(),
                    expression = expression,
                    result = result,
                )

                val builder: HistoryStore.Builder = store
                    .toBuilder()
                    .addHistories(0, newHistory.toProto())
                while (builder.historiesCount > HistoryConstraints.MAX_HISTORIES_SIZE) {
                    builder.removeHistories(builder.historiesCount - 1)
                }
                builder.build()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(HistoryRepositoryError.Add)
        }

        return Result.Ok(Unit)
    }

    suspend fun deleteHistory(id: String): Result<Unit, HistoryRepositoryError> {
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(HistoryRepositoryError.Delete)
        }

        return Result.Ok(Unit)
    }

    suspend fun deleteHistoryAll(): Result<Unit, HistoryRepositoryError> {
        try {
            dataStore.updateData { store ->
                store.toBuilder()
                    .clearHistories()
                    .build()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(HistoryRepositoryError.DeleteAll)
        }

        return Result.Ok(Unit)
    }
}

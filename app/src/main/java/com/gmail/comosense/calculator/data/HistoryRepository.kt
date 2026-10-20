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
    InvalidHistory,
}

class HistoryRepository(
    private val dataStore: DataStore<HistoryStore>,
) {
    val histories: Flow<List<History>> =
        dataStore.data.map { store ->
            store.historiesList
                .take(HistoryConstraints.MAX_HISTORY_COUNT)
                .mapNotNull { protoHistory -> protoHistory.toHistoryOrNull() }
        }

    suspend fun addHistory(
        expression: List<Symbol>,
        result: List<Symbol>,
    ): Result<Unit, HistoryRepositoryError> {
        if (expression.isEmpty() || result.isEmpty()) {
            return Result.Err(HistoryRepositoryError.InvalidHistory)
        }

        if (expression.size > HistoryConstraints.MAX_EXPRESSION_SIZE ||
            result.size > HistoryConstraints.MAX_RESULT_SIZE
        ) {
            return Result.Err(HistoryRepositoryError.TooLarge)
        }

        return updateData(
            error = HistoryRepositoryError.Add,
            transform = { store ->
                val newHistory = History(
                    id = UUID.randomUUID().toString(),
                    expression = expression,
                    result = result,
                )

                val builder: HistoryStore.Builder = store
                    .toBuilder()
                    .addHistories(0, newHistory.toProtoHistory())
                while (builder.historiesCount > HistoryConstraints.MAX_HISTORY_COUNT) {
                    builder.removeHistories(builder.historiesCount - 1)
                }
                builder.build()
            }
        )
    }

    suspend fun deleteHistory(
        id: String,
    ): Result<Unit, HistoryRepositoryError> {
        return updateData(
            error = HistoryRepositoryError.Delete,
            transform = { store ->
                val index: Int = store.historiesList
                    .indexOfFirst { history -> history.id == id }

                if (index < 0) {
                    store
                } else {
                    store.toBuilder()
                        .removeHistories(index)
                        .build()
                }
            }
        )
    }

    suspend fun deleteHistoryAll(): Result<Unit, HistoryRepositoryError> {
        return updateData(
            error = HistoryRepositoryError.DeleteAll,
            transform = { store ->
                store.toBuilder()
                    .clearHistories()
                    .build()
            }
        )
    }

    private suspend fun updateData(
        error: HistoryRepositoryError,
        transform: suspend (HistoryStore) -> HistoryStore,
    ): Result<Unit, HistoryRepositoryError> {
        try {
            dataStore.updateData(transform)
            return Result.Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return Result.Err(error)
        }
    }
}

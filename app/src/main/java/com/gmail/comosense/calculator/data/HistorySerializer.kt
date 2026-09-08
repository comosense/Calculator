package com.gmail.comosense.calculator.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.gmail.comosense.calculator.data.proto.HistoryStore
import java.io.InputStream
import java.io.OutputStream

object HistorySerializer : Serializer<HistoryStore> {
    override val defaultValue: HistoryStore = HistoryStore.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): HistoryStore {
        try {
            return HistoryStore.parseFrom(input)
        } catch (e: Exception) {
            throw CorruptionException("Cannot read history.", e)
        }
    }

    override suspend fun writeTo(t: HistoryStore, output: OutputStream) {
        t.writeTo(output)
    }
}

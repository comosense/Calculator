package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.domain.Symbol

data class History(
    val id: String,
    val expression: List<Symbol>,
    val result: List<Symbol>,
)

internal object HistoryConstraints {
    const val MAX_HISTORIES_SIZE: Int = 50
    const val MAX_EXPRESSION_SIZE: Int = 1024
    const val MAX_RESULT_SIZE: Int = 256
}

package com.gmail.comosense.calculator.data

import com.gmail.comosense.calculator.common.Constraints
import com.gmail.comosense.calculator.domain.Symbol

internal object HistoryConstraints {
    const val MAX_HISTORIES_SIZE: Int = Constraints.MAX_HISTORY_SIZE
    const val MAX_EXPRESSION_SIZE: Int = Constraints.MAX_EXPRESSION_SIZE
    const val MAX_RESULT_SIZE: Int = Constraints.MAX_RESULT_SIZE
}

data class History(
    val id: String,
    val expression: List<Symbol>,
    val result: List<Symbol>,
)

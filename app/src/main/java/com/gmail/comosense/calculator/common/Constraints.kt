package com.gmail.comosense.calculator.common

internal object Constraints {
    const val DEFAULT_PRECISION: Int = 16
    const val MIN_PRECISION: Int = 8
    const val MAX_PRECISION: Int = 64
    const val MAX_HISTORY_COUNT: Int = 50
    const val MAX_RESULT_SIZE: Int = 256
    const val MAX_EXPRESSION_SIZE: Int = MAX_RESULT_SIZE * 4
}

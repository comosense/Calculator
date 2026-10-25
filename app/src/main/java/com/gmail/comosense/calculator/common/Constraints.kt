package com.gmail.comosense.calculator.common

internal object Constraints {
    const val DEFAULT_PRECISION: Int = 16
    const val MIN_PRECISION: Int = 4
    const val MAX_HISTORY_COUNT: Int = 50
    const val MAX_RESULT_SIZE: Int = 512
    const val MAX_EXPRESSION_SIZE: Int = 1024
    const val MAX_NESTING_DEPTH: Int = 64
    const val MAX_FACTORIAL_ARGUMENT: Int = 253
    const val MAX_INTEGER_EXPONENT: Int = 1024
    const val MAX_NON_INTEGER_EXPONENT: Int = 1024
    val VALID_PRECISIONS: List<Int> = listOf(8, 16, 24, 32, 40, 48, 56, 64)

    init {
        require(VALID_PRECISIONS.distinct().size == VALID_PRECISIONS.size)
        require(VALID_PRECISIONS.all { precision -> precision >= MIN_PRECISION })
        require(DEFAULT_PRECISION in VALID_PRECISIONS)
    }
}

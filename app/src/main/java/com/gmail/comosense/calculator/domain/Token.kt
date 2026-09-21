package com.gmail.comosense.calculator.domain

import java.math.BigDecimal

sealed interface Token {
    sealed interface Sign : Token {
        data object Positive : Sign
        data object Negative : Sign
    }

    data class Numeric(val value: BigDecimal) : Token

    sealed interface FactorStart : Token {
        data object OpeningParenthesis : FactorStart
    }

    sealed interface FactorEnd : Token {
        data object ClosingParenthesis : FactorEnd
    }

    sealed interface Operator : Token {
        data object Add : Operator
        data object Subtract : Operator
        data object Multiply : Operator
        data object Divide : Operator
    }
}

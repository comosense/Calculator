package com.gmail.comosense.calculator.presentation

sealed interface Symbol {
    sealed interface Sign : Symbol {
        data object Positive : Sign
        data object Negative : Sign
    }

    sealed interface Numeric : Symbol {
        data class Digit(val value: Int) : Numeric
        data object Point : Numeric
    }

    sealed interface FactorStart : Symbol {
        data object OpenParenthesis : FactorStart
    }

    sealed interface FactorEnd : Symbol {
        data object CloseParenthesis : FactorEnd
    }

    sealed interface Operator : Symbol {
        data object Add : Operator
        data object Subtract : Operator
        data object Multiply : Operator
        data object Divide : Operator
    }

    data class Error(val value: String) : Symbol
}

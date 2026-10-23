package com.gmail.comosense.calculator.domain

import java.math.BigDecimal

sealed interface Token {
    sealed interface Sign : Token {
        data object Positive : Sign
        data object Negative : Sign
    }

    data class Numeric(val value: BigDecimal) : Token

    sealed interface Constant : Token {
        data object Euler : Constant
        data object Pi : Constant
    }

    sealed interface Factor : Token {
        sealed interface Function : Factor {
            data object Sqrt : Function
        }
    }

    sealed interface FactorStart : Token {
        data object OpeningParenthesis : FactorStart
        sealed interface Function : FactorStart {
            data object Ln : Function
            data object Log : Function
            data object Sin : Function
            data object Cos : Function
            data object Tan : Function
            data object Arcsin : Function
            data object Arccos : Function
            data object Arctan : Function
        }
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

    sealed interface SpecialOperator : Token {
        data object Power : SpecialOperator
        data object Factorial : SpecialOperator
    }
}

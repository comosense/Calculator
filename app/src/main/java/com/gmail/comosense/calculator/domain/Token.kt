package com.gmail.comosense.calculator.domain

import java.math.BigDecimal

sealed interface Token {
    sealed interface Sign : Token {
        data object Positive : Sign
        data object Negative : Sign
    }

    data class Numeric(val value: BigDecimal) : Token

    sealed interface Constant : Token {
        data object Pi : Constant
        data object Euler : Constant
    }

    sealed interface Factor : Token {
        sealed interface Function : Factor {
            data object Sqrt : Function
        }
    }

    sealed interface FactorStart : Token {
        data object OpeningParenthesis : FactorStart
        sealed interface Function : FactorStart {
            data object Sin : Function
            data object Cos : Function
            data object Tan : Function
            data object Log : Function
            data object Ln : Function
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
        data object Power : Operator
    }
}

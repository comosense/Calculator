package com.gmail.comosense.calculator.domain

sealed interface Symbol {
    sealed interface Sign : Symbol {
        data object Positive : Sign
        data object Negative : Sign
    }

    sealed interface Numeric : Symbol {
        data class Digit(val value: Int) : Numeric {
            init {
                require(value in 0..9)
            }
        }

        data object Point : Numeric
    }

    sealed interface Constant : Symbol {
        data object Pi : Constant
        data object Euler : Constant
    }

    sealed interface Factor : Symbol {
        sealed interface Function : Factor {
            data object Sqrt : Function
        }
    }

    sealed interface FactorStart : Symbol {
        data object OpeningParenthesis : FactorStart
        sealed interface Function : FactorStart {
            data object Sin : Function
            data object Cos : Function
            data object Tan : Function
            data object Log : Function
            data object Ln : Function
        }
    }

    sealed interface FactorEnd : Symbol {
        data object ClosingParenthesis : FactorEnd
    }

    sealed interface Operator : Symbol {
        data object Add : Operator
        data object Subtract : Operator
        data object Multiply : Operator
        data object Divide : Operator
        data object Power : Operator
    }
}

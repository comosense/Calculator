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
        data object Euler : Constant
        data object Pi : Constant
    }

    sealed interface Factor : Symbol {
        sealed interface Function : Factor {
            data object Sqrt : Function
            data object Pow10 : Function
        }
    }

    sealed interface FactorStart : Symbol {
        data object OpeningParenthesis : FactorStart
        sealed interface Function : FactorStart {
            data object Ln : Function
            data object Log : Function
            data object Sin : Function
            data object Cos : Function
            data object Tan : Function
            data object Exp : Function
            data object Arcsin : Function
            data object Arccos : Function
            data object Arctan : Function
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

    sealed interface PostfixOperator : Symbol {
        data object Factorial : PostfixOperator
    }
}

package com.gmail.comosense.calculator.presentation

sealed interface Symbol {
    val text: String

    sealed interface Sign : Symbol {
        data object Positive : Sign {
            override val text: String = "+"
        }

        data object Negative : Sign {
            override val text: String = "-"
        }
    }

    sealed interface Numeric : Symbol {
        data class Digit(val value: Int) : Numeric {
            override val text: String = value.toString()
        }

        data object Point : Numeric {
            override val text: String = "."
        }
    }

    sealed interface FactorStart : Symbol {
        data object OpenParenthesis : FactorStart {
            override val text: String = "("
        }
    }

    sealed interface FactorEnd : Symbol {
        data object CloseParenthesis : FactorEnd {
            override val text: String = ")"
        }
    }

    sealed interface Operator : Symbol {
        data object Add : Operator {
            override val text: String = "+"
        }

        data object Subtract : Operator {
            override val text: String = "-"
        }

        data object Multiply : Operator {
            override val text: String = "×"
        }

        data object Divide : Operator {
            override val text: String = "÷"
        }
    }

    data class Error(val value: String) : Symbol {
        override val text: String = value
    }
}

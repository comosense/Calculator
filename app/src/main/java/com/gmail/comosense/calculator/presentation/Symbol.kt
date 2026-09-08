package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.SymbolText

sealed interface Symbol {
    val text: String

    sealed interface Sign : Symbol {
        data object Positive : Sign {
            override val text: String = SymbolText.POSITIVE
        }

        data object Negative : Sign {
            override val text: String = SymbolText.NEGATIVE
        }
    }

    sealed interface Numeric : Symbol {
        data class Digit(val value: Int) : Numeric {
            override val text: String = value.toString()
        }

        data object Point : Numeric {
            override val text: String = SymbolText.POINT
        }
    }

    sealed interface FactorStart : Symbol {
        data object OpenParenthesis : FactorStart {
            override val text: String = SymbolText.OPEN_PARENTHESIS
        }
    }

    sealed interface FactorEnd : Symbol {
        data object CloseParenthesis : FactorEnd {
            override val text: String = SymbolText.CLOSE_PARENTHESIS
        }
    }

    sealed interface Operator : Symbol {
        data object Add : Operator {
            override val text: String = SymbolText.ADD
        }

        data object Subtract : Operator {
            override val text: String = SymbolText.SUBTRACT
        }

        data object Multiply : Operator {
            override val text: String = SymbolText.MULTIPLY
        }

        data object Divide : Operator {
            override val text: String = SymbolText.DIVIDE
        }
    }

    data class Error(val value: String) : Symbol {
        override val text: String = value
    }
}

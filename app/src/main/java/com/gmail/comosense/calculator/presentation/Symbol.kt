package com.gmail.comosense.calculator.presentation

import com.gmail.comosense.calculator.common.MathCharacter

sealed interface Symbol {
    val text: String

    sealed interface Sign : Symbol {
        data object Positive : Sign {
            override val text: String = MathCharacter.POSITIVE
        }

        data object Negative : Sign {
            override val text: String = MathCharacter.NEGATIVE
        }
    }

    sealed interface Numeric : Symbol {
        data class Digit(val value: Int) : Numeric {
            override val text: String = value.toString()
        }

        data object Point : Numeric {
            override val text: String = MathCharacter.POINT
        }
    }

    sealed interface FactorStart : Symbol {
        data object OpenParenthesis : FactorStart {
            override val text: String = MathCharacter.OPEN_PARENTHESIS
        }
    }

    sealed interface FactorEnd : Symbol {
        data object CloseParenthesis : FactorEnd {
            override val text: String = MathCharacter.CLOSE_PARENTHESIS
        }
    }

    sealed interface Operator : Symbol {
        data object Add : Operator {
            override val text: String = MathCharacter.ADD
        }

        data object Subtract : Operator {
            override val text: String = MathCharacter.SUBTRACT
        }

        data object Multiply : Operator {
            override val text: String = MathCharacter.MULTIPLY
        }

        data object Divide : Operator {
            override val text: String = MathCharacter.DIVIDE
        }
    }

    data class Error(val value: String) : Symbol {
        override val text: String = value
    }
}

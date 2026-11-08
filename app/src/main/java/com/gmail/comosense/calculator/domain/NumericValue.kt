package com.gmail.comosense.calculator.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class DivisionByZeroException : ArithmeticException()

sealed interface NumericValue {
    data class RationalValue(
        val value: Rational,
    ) : NumericValue

    data class DecimalValue(
        val value: BigDecimal,
    ) : NumericValue
}

private fun NumericValue.asRationalOrNull(): Rational? = when (this) {
    is NumericValue.RationalValue ->
        value

    is NumericValue.DecimalValue ->
        null
}

internal fun NumericValue.toBigDecimal(
    mc: MathContext,
): BigDecimal = when (this) {
    is NumericValue.RationalValue ->
        value.toBigDecimal(mc)

    is NumericValue.DecimalValue ->
        value
}

internal fun NumericValue.toBigIntegerExact(): BigInteger = when (this) {
    is NumericValue.RationalValue ->
        value.toBigIntegerExact()

    is NumericValue.DecimalValue ->
        value.toBigIntegerExact()
}

internal fun NumericValue.signum(): Int = when (this) {
    is NumericValue.RationalValue ->
        value.signum()

    is NumericValue.DecimalValue ->
        value.signum()
}

internal fun NumericValue.negate(): NumericValue = when (this) {
    is NumericValue.RationalValue ->
        NumericValue.RationalValue(value.negate())

    is NumericValue.DecimalValue ->
        NumericValue.DecimalValue(value.negate())
}

internal fun NumericValue.add(
    other: NumericValue,
    mc: MathContext,
): NumericValue {
    val left: Rational? = asRationalOrNull()
    val right: Rational? = other.asRationalOrNull()

    return if (left != null && right != null) {
        NumericValue.RationalValue(left + right)
    } else {
        NumericValue.DecimalValue(
            toBigDecimal(mc).add(other.toBigDecimal(mc))
        )
    }
}

internal fun NumericValue.subtract(
    other: NumericValue,
    mc: MathContext,
): NumericValue {
    val left: Rational? = asRationalOrNull()
    val right: Rational? = other.asRationalOrNull()

    return if (left != null && right != null) {
        NumericValue.RationalValue(left - right)
    } else {
        NumericValue.DecimalValue(
            toBigDecimal(mc).subtract(other.toBigDecimal(mc))
        )
    }
}

internal fun NumericValue.multiply(
    other: NumericValue,
    mc: MathContext,
): NumericValue {
    val left: Rational? = asRationalOrNull()
    val right: Rational? = other.asRationalOrNull()

    return if (left != null && right != null) {
        NumericValue.RationalValue(left * right)
    } else {
        NumericValue.DecimalValue(
            toBigDecimal(mc).multiply(other.toBigDecimal(mc))
        )
    }
}

internal fun NumericValue.divide(
    other: NumericValue,
    mc: MathContext,
): NumericValue {
    val left: Rational? = asRationalOrNull()
    val right: Rational? = other.asRationalOrNull()

    if (right?.isZero() == true ||
        (right == null && other.toBigDecimal(mc).signum() == 0)
    ) {
        throw DivisionByZeroException()
    }

    return if (left != null && right != null) {
        NumericValue.RationalValue(left / right)
    } else {
        NumericValue.DecimalValue(
            toBigDecimal(mc).divide(other.toBigDecimal(mc), mc)
        )
    }
}

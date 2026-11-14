package com.gmail.comosense.calculator.domain

import com.gmail.comosense.calculator.common.Result
import kotlinx.coroutines.CancellationException
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class CalculatorError {
    Unsupported,
    ResultTooLarge,
    InsufficientPrecision,
    InvalidExpression,
    DivisionByZero,
    FactorialInvalidArgument,
    FactorialLargeArgument,
    SqrtInvalidArgument,
    ExpLargeArgument,
    PowInvalidArgument,
    PowLimitExceeded,
    LogInvalidArgument,
    TanInvalidArgument,
    ArcsinInvalidArgument,
    ArccosInvalidArgument,
    Arithmetic,
}

private const val EXTRA_PRECISION_FOR_WORKING: Int = 2

suspend fun calculate(
    expression: List<Symbol>,
    isRadians: Boolean,
    precision: Int,
): Result<List<Symbol>, CalculatorError> {
    require(precision > 0)

    val calculatedResult: Result<BigDecimal, CalculatorError> =
        parse(
            expression = expression,
            isRadians = isRadians,
            precision = precision,
        )

    val calculated: BigDecimal = when (calculatedResult) {
        is Result.Ok ->
            calculatedResult.value

        is Result.Err ->
            return Result.Err(calculatedResult.error)
    }

    val calculatedSymbolsResult: Result<List<Symbol>, SymbolizerError> =
        calculated.toSymbols()

    return when (calculatedSymbolsResult) {
        is Result.Ok ->
            Result.Ok(calculatedSymbolsResult.value)

        is Result.Err ->
            Result.Err(calculatedSymbolsResult.error.toCalculatorError())
    }
}

private suspend fun parse(
    expression: List<Symbol>,
    isRadians: Boolean,
    precision: Int,
): Result<BigDecimal, CalculatorError> {
    require(precision > 0)

    if (expression.isEmpty()) {
        return Result.Err(CalculatorError.InvalidExpression)
    }

    val parser = Parser(
        expression = expression,
        isRadians = isRadians,
    )
    val workingMc = MathContext(
        precision + EXTRA_PRECISION_FOR_WORKING,
        RoundingMode.HALF_UP,
    )
    val mc = MathContext(
        precision,
        RoundingMode.HALF_UP,
    )

    return try {
        Result.Ok(
            parser
                .parse(workingMc)
                .toBigDecimal(mc),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: InvalidNumericFormatException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: MissingFactorException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: IllegalFactorException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: FactorialLargeArgumentException) {
        Result.Err(CalculatorError.FactorialLargeArgument)
    } catch (_: FactorialInvalidArgumentException) {
        Result.Err(CalculatorError.FactorialInvalidArgument)
    } catch (_: ExpLargeArgumentException) {
        Result.Err(CalculatorError.ExpLargeArgument)
    } catch (_: PowInvalidArgumentException) {
        Result.Err(CalculatorError.PowInvalidArgument)
    } catch (_: PowLimitExceededException) {
        Result.Err(CalculatorError.PowLimitExceeded)
    } catch (_: DivisionByZeroException) {
        Result.Err(CalculatorError.DivisionByZero)
    } catch (_: SqrtInvalidArgumentException) {
        Result.Err(CalculatorError.SqrtInvalidArgument)
    } catch (_: LogInvalidArgumentException) {
        Result.Err(CalculatorError.LogInvalidArgument)
    } catch (_: TanInvalidArgumentException) {
        Result.Err(CalculatorError.TanInvalidArgument)
    } catch (_: ArcsinInvalidArgumentException) {
        Result.Err(CalculatorError.ArcsinInvalidArgument)
    } catch (_: ArccosInvalidArgumentException) {
        Result.Err(CalculatorError.ArccosInvalidArgument)
    } catch (_: IllegalArgumentException) {
        Result.Err(CalculatorError.InvalidExpression)
    } catch (_: ArithmeticException) {
        Result.Err(CalculatorError.Arithmetic)
    }
}

private fun SymbolizerError.toCalculatorError(): CalculatorError = when (this) {
    SymbolizerError.Unsupported ->
        CalculatorError.Unsupported

    SymbolizerError.TooLarge ->
        CalculatorError.ResultTooLarge
}

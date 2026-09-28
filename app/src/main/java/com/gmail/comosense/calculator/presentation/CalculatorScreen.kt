package com.gmail.comosense.calculator.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.AppTheme
import kotlin.math.sqrt

@Composable
fun CalculatorScreen(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    ScreenScaffold {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.calculatorScreenColors.background),
            contentAlignment = Alignment.Center,
        ) {
            val shortLength: Dp = minOf(maxWidth, maxHeight) * (1f / sqrt(2f))
            val longLength: Dp = minOf(maxWidth, maxHeight) * (sqrt(3f) / 2f)

            MainPanel(
                appState = appState,
                shortLength = shortLength,
                longLength = longLength,
                onAction = onAction,
                onShowHistory = onShowHistory,
                modifier = Modifier.size(
                    width = longLength,
                    height = shortLength,
                ),
                symbolFormatter = symbolFormatter,
            )
        }
    }
}

@Composable
private fun MainPanel(
    appState: AppState,
    shortLength: Dp,
    longLength: Dp,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier,
    symbolFormatter: SymbolFormatter,
) {
    val deleteKey: Key =
        if (appState.isEntering) {
            Key.Backspace
        } else {
            Key.Clear
        }
    val parenthesisKey: Key =
        if (appState.canAppend(Symbol.FactorEnd.ClosingParenthesis)) {
            Key.ClosingParenthesis
        } else {
            Key.OpeningParenthesis
        }
    val plusKey: Key =
        if (appState.canAppend(Symbol.Operator.Add)) {
            Key.Add
        } else {
            Key.Positive
        }
    val minusKey: Key =
        if (appState.canAppend(Symbol.Operator.Subtract)) {
            Key.Subtract
        } else {
            Key.Negative
        }
    val mainKeyGrid: List<List<Key?>> = listOf(
        listOf(
            Key.Digit(7),
            Key.Digit(8),
            Key.Digit(9),
            deleteKey,
            Key.Equal,
        ),
        listOf(
            Key.Digit(4),
            Key.Digit(5),
            Key.Digit(6),
            Key.Multiply,
            Key.Divide,
        ),
        listOf(
            Key.Digit(1),
            Key.Digit(2),
            Key.Digit(3),
            plusKey,
            minusKey,
        ),
        listOf(
            null,
            Key.Digit(0),
            Key.Point,
            parenthesisKey,
            null,
        ),
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ExpressionBox(
                appState = appState,
                onShowHistory = onShowHistory,
                modifier = Modifier
                    .size(shortLength)
                    .weight(1f),
                symbolFormatter = symbolFormatter,
            )

            KeyGrid(
                keyGrid = mainKeyGrid,
                onClick = { key ->
                    onAction(key.appAction)
                },
                modifier = Modifier
                    .size(longLength)
                    .weight(4f),
                arrangementSpace = 2.dp,
                symbolFormatter = symbolFormatter,
            )
        }
    }
}

@Composable
private fun ExpressionBox(
    appState: AppState,
    onShowHistory: () -> Unit,
    modifier: Modifier,
    symbolFormatter: SymbolFormatter,
) {
    val scrollState: ScrollState = rememberScrollState()
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    val expression: String = symbolFormatter.format(appState.expression).ifEmpty { "0" }

    BoxWithConstraints(
        modifier = modifier
            .clickable { onShowHistory() },
        contentAlignment = Alignment.Center,
    ) {
        val density: Density = LocalDensity.current
        val minFontSize: TextUnit = AppTheme.calculatorScreenDimensions.expressionMinFontSize
        val measured: TextLayoutResult = remember(
            expression,
            minFontSize,
            maxWidth,
        ) {
            textMeasurer.measure(
                text = expression,
                style = TextStyle(
                    fontSize = minFontSize,
                ),
            )
        }

        val needScroll: Boolean = with(density) {
            measured.size.width > maxWidth.toPx()
        }

        LaunchedEffect(
            expression,
            needScroll,
            scrollState.maxValue,
        ) {
            if (needScroll) {
                scrollState.scrollTo(scrollState.maxValue)
            }
        }

        if (needScroll) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
            ) {
                Text(
                    text = expression,
                    maxLines = 1,
                    color = AppTheme.calculatorScreenColors.expression,
                    fontSize = AppTheme.calculatorScreenDimensions.expressionMinFontSize,
                )
            }
        } else {
            Text(
                text = expression,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = AppTheme.calculatorScreenDimensions.expressionMaxFontSize,
                    minFontSize = AppTheme.calculatorScreenDimensions.expressionMinFontSize,
                ),
                maxLines = 1,
                color = AppTheme.calculatorScreenColors.expression,
            )
        }
    }
}

@Composable
private fun KeyGrid(
    keyGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    modifier: Modifier,
    arrangementSpace: Dp,
    symbolFormatter: SymbolFormatter,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(arrangementSpace)
    ) {
        keyGrid.forEach { keys ->
            KeysRow(
                keys = keys,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                arrangementSpace = arrangementSpace,
                symbolFormatter = symbolFormatter,
            )
        }
    }
}

@Composable
private fun KeysRow(
    keys: List<Key?>,
    onClick: (Key) -> Unit,
    modifier: Modifier,
    arrangementSpace: Dp,
    symbolFormatter: SymbolFormatter,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(arrangementSpace),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { key ->
            if (key != null) {
                KeyButton(
                    key = key,
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    symbolFormatter = symbolFormatter,
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    key: Key,
    onClick: (Key) -> Unit,
    modifier: Modifier,
    symbolFormatter: SymbolFormatter,
) {
    data class Colors(
        val container: Color,
        val content: Color,
    )

    val colors: Colors = when (key.style) {
        Style.Digit ->
            Colors(
                container = AppTheme.calculatorScreenColors.digitKeyContainer,
                content = AppTheme.calculatorScreenColors.digitKeyContent,
            )

        Style.Operator ->
            Colors(
                container = AppTheme.calculatorScreenColors.operatorKeyContainer,
                content = AppTheme.calculatorScreenColors.operatorKeyContent,
            )

        Style.Command ->
            Colors(
                container = AppTheme.calculatorScreenColors.commandKeyContainer,
                content = AppTheme.calculatorScreenColors.commandKeyContent,
            )
    }

    Button(
        onClick = { onClick(key) },
        onLongClick = { key.longClickKeyOrNull?.let { onClick(it) } },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.container,
            contentColor = colors.content,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (val display: Display = key.display(symbolFormatter)) {
                is Display.Text -> {
                    Text(
                        text = display.text,
                        color = colors.content,
                        fontSize = AppTheme.calculatorScreenDimensions.keyTextFontSize,
                        textAlign = TextAlign.Center,
                    )
                }

                is Display.Icon -> {
                    Icon(
                        painter = painterResource(display.painterResource),
                        contentDescription = stringResource(display.stringResource),
                        modifier = Modifier.size(AppTheme.calculatorScreenDimensions.keyIconSize),
                        tint = colors.content,
                    )
                }
            }
        }
    }
}

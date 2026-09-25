package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.zIndex
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import kotlin.math.sqrt

@Composable
fun CalculatorScreen(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    val mainBoxSizeRatio: Float = 1f / sqrt(2f)

    ScreenScaffold {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(CalculatorTheme.calculatorScreenColors.background),
            contentAlignment = Alignment.Center,
        ) {
            MainPanel(
                appState = appState,
                onAction = onAction,
                onShowHistory = onShowHistory,
                modifier = Modifier.size(
                    minOf(maxWidth, maxHeight) * mainBoxSizeRatio
                ),
                symbolFormatter = symbolFormatter,
            )
        }
    }
}

@Composable
private fun MainPanel(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier,
    symbolFormatter: SymbolFormatter,
) {
    var showOperatorsKeyGrid: Boolean by remember { mutableStateOf(false) }

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
        ),
        listOf(
            Key.Digit(4),
            Key.Digit(5),
            Key.Digit(6),
            Key.Equal,
        ),
        listOf(
            Key.Digit(1),
            Key.Digit(2),
            Key.Digit(3),
            Key.Operators,
        ),
        listOf(
            Key.Digit(0),
            Key.Point,
            parenthesisKey,
            null,
        ),
    )
    val operatorsKeyGrid: List<List<Key?>> = listOf(
        listOf(
            Key.Multiply,
            Key.Divide,
        ),
        listOf(
            plusKey,
            minusKey,
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
                    .fillMaxWidth()
                    .weight(1f),
                symbolFormatter = symbolFormatter,
            )

            KeyGrid(
                keyGrid = mainKeyGrid,
                onClick = { key ->
                    when (key) {
                        is Key.Operators ->
                            showOperatorsKeyGrid = true

                        else ->
                            key.appActionOrNull?.let(onAction)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4f),
                arrangementSpace = 2.dp,
                symbolFormatter = symbolFormatter,
            )
        }

        AnimatedVisibility(
            visible = showOperatorsKeyGrid,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f),
            enter = fadeIn(
                animationSpec = tween(180),
            ) + scaleIn(
                initialScale = 0.96f,
                animationSpec = tween(180),
            ),
            exit = fadeOut(
                animationSpec = tween(180),
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(180),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showOperatorsKeyGrid = false },
                contentAlignment = Alignment.Center,
            ) {
                KeyGrid(
                    keyGrid = operatorsKeyGrid,
                    onClick = { key ->
                        showOperatorsKeyGrid = false
                        key.appActionOrNull?.let(onAction)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(32.dp),
                    arrangementSpace = 8.dp,
                    symbolFormatter = symbolFormatter,
                )
            }
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
        val minFontSize: TextUnit = CalculatorTheme.calculatorScreenDimensions.expressionMinFontSize
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
                    color = CalculatorTheme.calculatorScreenColors.expression,
                    fontSize = CalculatorTheme.calculatorScreenDimensions.expressionMinFontSize,
                )
            }
        } else {
            Text(
                text = expression,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = CalculatorTheme.calculatorScreenDimensions.expressionMaxFontSize,
                    minFontSize = CalculatorTheme.calculatorScreenDimensions.expressionMinFontSize,
                ),
                maxLines = 1,
                color = CalculatorTheme.calculatorScreenColors.expression,
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
    Button(
        onClick = { onClick(key) },
        onLongClick = { key.longClickKeyOrNull?.let { onClick(it) } },
        modifier = modifier,
        colors = key.colors,
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
                        color = key.colors.contentColor,
                        fontSize = CalculatorTheme.calculatorScreenDimensions.keyTextSize,
                        textAlign = TextAlign.Center,
                    )
                }

                is Display.Icon -> {
                    Icon(
                        painter = painterResource(display.painterResource),
                        contentDescription = stringResource(display.stringResource),
                        modifier = Modifier.size(CalculatorTheme.calculatorScreenDimensions.keyIconSize),
                        tint = key.colors.contentColor,
                    )
                }
            }
        }
    }
}

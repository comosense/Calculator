package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun CalculatorScreen(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    locale: Locale,
) {
    val mainBoxSizeRatio: Float = 1f / sqrt(2f)

    AppScaffold {
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
                    locale = locale,
                )
            }
        }
    }
}

@Composable
private fun MainPanel(
    appState: AppState,
    onAction: (AppAction) -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier,
    locale: Locale,
) {
    var showOperatorsKeyGrid: Boolean by remember { mutableStateOf(false) }

    val expressionMaxFontSize: TextUnit = 32.sp
    val expressionMinFontSize: TextUnit = 16.sp
    val keyTextSize: TextUnit = 18.sp

    val deleteKey: CommandKey =
        if (appState.isEntering) {
            CommandKey.Backspace
        } else {
            CommandKey.Clear
        }
    val parenthesisKey: SymbolKey =
        if (appState.canAppend(Symbol.FactorEnd.CloseParenthesis)) {
            SymbolKey.CloseParenthesis
        } else {
            SymbolKey.OpenParenthesis
        }
    val plusKey: SymbolKey =
        if (appState.canAppend(Symbol.Operator.Add)) {
            SymbolKey.Add
        } else {
            SymbolKey.Positive
        }
    val minusKey: SymbolKey =
        if (appState.canAppend(Symbol.Operator.Subtract)) {
            SymbolKey.Subtract
        } else {
            SymbolKey.Negative
        }
    val mainKeysGrid: List<List<Key?>> = listOf(
        listOf(
            SymbolKey.Digit(7),
            SymbolKey.Digit(8),
            SymbolKey.Digit(9),
            deleteKey,
        ),
        listOf(
            SymbolKey.Digit(4),
            SymbolKey.Digit(5),
            SymbolKey.Digit(6),
            CommandKey.Equal,
        ),
        listOf(
            SymbolKey.Digit(1),
            SymbolKey.Digit(2),
            SymbolKey.Digit(3),
            UiKey.Operators,
        ),
        listOf(
            SymbolKey.Digit(0),
            SymbolKey.Point,
            parenthesisKey,
            null,
        ),
    )
    val operatorsKeysGrid: List<List<Key?>> = listOf(
        listOf(
            SymbolKey.Multiply,
            SymbolKey.Divide,
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
                maxFontSize = expressionMaxFontSize,
                minFontSize = expressionMinFontSize,
                locale = locale,
            )

            KeysGrid(
                keysGrid = mainKeysGrid,
                onClick = { key ->
                    when (key) {
                        is UiKey.Operators ->
                            showOperatorsKeyGrid = true

                        else ->
                            key.toAppAction?.let(onAction)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(4f),
                keyTextSize = keyTextSize,
                arrangementSpace = 2.dp,
                locale = locale,
            )

        }

        AnimatedVisibility(
            visible = showOperatorsKeyGrid,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f),
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.75f),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { showOperatorsKeyGrid = false },
            ) {
                KeysGrid(
                    keysGrid = operatorsKeysGrid,
                    onClick = { key ->
                        showOperatorsKeyGrid = false
                        key.toAppAction?.let(onAction)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    keyTextSize = keyTextSize,
                    arrangementSpace = 8.dp,
                    locale = locale,
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
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    locale: Locale,
) {
    val expression: String = appState.displayExpression(locale)
    val scrollState: ScrollState = rememberScrollState()
    val textMeasurer: TextMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier
            .clickable { onShowHistory() },
        contentAlignment = Alignment.Center,
    ) {
        val density: Density = LocalDensity.current
        val measured: TextLayoutResult = remember(
            expression,
            minFontSize,
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
                    fontSize = minFontSize,
                )
            }
        } else {
            Text(
                text = expression,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = maxFontSize,
                    minFontSize = minFontSize,
                ),
                maxLines = 1,
                color = CalculatorTheme.calculatorScreenColors.expression,
            )
        }
    }
}

@Composable
private fun KeysGrid(
    keysGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    modifier: Modifier,
    keyTextSize: TextUnit,
    arrangementSpace: Dp,
    locale: Locale,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(arrangementSpace)
    ) {
        keysGrid.forEach { keys ->
            KeysRow(
                keys = keys,
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                keyTextSize = keyTextSize,
                arrangementSpace = arrangementSpace,
                locale = locale,
            )
        }
    }
}

@Composable
private fun KeysRow(
    keys: List<Key?>,
    onClick: (Key) -> Unit,
    modifier: Modifier,
    keyTextSize: TextUnit,
    arrangementSpace: Dp,
    locale: Locale,
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
                    keyTextSize = keyTextSize,
                    locale = locale,
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
    keyTextSize: TextUnit,
    locale: Locale,
) {
    Button(
        onClick = { onClick(key) },
        onLongClick = { key.longClickKey?.let { onClick(it) } },
        modifier = modifier,
        colors = key.colors,
    ) {
        when (val display: Display = key.display(locale)) {
            is Display.Text -> {
                Text(
                    text = display.text,
                    color = key.colors.contentColor,
                    fontSize = keyTextSize,
                    textAlign = TextAlign.Center,
                )
            }

            is Display.Drawable -> {
                Icon(
                    painter = painterResource(display.painterResource),
                    contentDescription = stringResource(display.stringResource),
                    tint = key.colors.contentColor,
                )
            }
        }
    }
}

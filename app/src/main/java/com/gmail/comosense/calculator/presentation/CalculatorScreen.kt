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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
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
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.gmail.comosense.calculator.domain.Symbol
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun CalculatorScreen(
    appState: AppState,
    onClick: (Key) -> Unit,
    onShowHistory: () -> Unit,
) {
    val locale: Locale = LocalLocale.current.platformLocale
    val mainBoxSizeRatio: Float = 1f / sqrt(2f)

    AppScaffold {
        ScreenScaffold {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val mainBoxSize: Dp = minOf(maxWidth, maxHeight) * mainBoxSizeRatio
                Box(
                    modifier = Modifier.size(mainBoxSize),
                ) {
                    MainBox(
                        appState = appState,
                        onClick = onClick,
                        onShowHistory = onShowHistory,
                        locale = locale,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBox(
    appState: AppState,
    onClick: (Key) -> Unit,
    onShowHistory: () -> Unit,
    locale: Locale,
) {
    var showOperatorKeyBox by remember { mutableStateOf(false) }
    val expressionTextColor: Color = MaterialTheme.colorScheme.onBackground
    val expressionMaxFontSize: TextUnit = 32.sp
    val expressionMinFontSize: TextUnit = 16.sp
    val keyTextSize: TextUnit = 18.sp
    val deleteKey: CommandKey =
        if (appState.isEntering) {
            CommandKey.Delete
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExpressionBox(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            appState = appState,
            color = expressionTextColor,
            maxFontSize = expressionMaxFontSize,
            minFontSize = expressionMinFontSize,
            onShowHistory = onShowHistory,
            locale = locale,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(4f),
        ) {
            KeyBox(
                modifier = Modifier.fillMaxSize(),
                arrangementSpace = 2.dp,
                keyTextSize = keyTextSize,
                keyGrid = listOf(
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
                        CommandKey.Equals,
                    ),
                    listOf(
                        SymbolKey.Digit(1),
                        SymbolKey.Digit(2),
                        SymbolKey.Digit(3),
                        ActionKey.OperatorBox,
                    ),
                    listOf(
                        SymbolKey.Digit(0),
                        SymbolKey.Point,
                        parenthesisKey,
                        null,
                    ),
                ),
                onClick = { key ->
                    if (key == ActionKey.OperatorBox) {
                        showOperatorKeyBox = true
                    } else {
                        onClick(key)
                    }
                },
                locale = locale,
            )

        }
    }

    AnimatedVisibility(
        visible = showOperatorKeyBox,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        enter = fadeIn() + scaleIn(initialScale = 0.25f),
        exit = fadeOut() + scaleOut(targetScale = 0.25f),
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.tertiaryDim.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { showOperatorKeyBox = false },
        ) {
            OperatorKeyBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                arrangementSpace = 8.dp,
                keyTextSize = keyTextSize,
                keyGrid = listOf(
                    listOf(
                        plusKey,
                        minusKey,
                    ),
                    listOf(
                        SymbolKey.Multiply,
                        SymbolKey.Divide,
                    ),
                ),
                onClick = { key ->
                    showOperatorKeyBox = false
                    onClick(key)
                },
                locale = locale,
            )
        }
    }
}

@Composable
private fun ExpressionBox(
    modifier: Modifier,
    appState: AppState,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    onShowHistory: () -> Unit,
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
                    color = color,
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
                BasicText(
                    text = expression,
                    maxLines = 1,
                    style = TextStyle(
                        color = color,
                        fontSize = minFontSize,
                    ),
                )
            }
        } else {
            BasicText(
                text = expression,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = maxFontSize,
                    minFontSize = minFontSize,
                ),
                maxLines = 1,
                style = TextStyle(
                    color = color,
                ),
            )
        }
    }
}

@Composable
private fun OperatorKeyBox(
    modifier: Modifier,
    arrangementSpace: Dp,
    keyTextSize: TextUnit,
    keyGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Box(
        modifier = modifier,
    ) {
        KeyBox(
            modifier = Modifier.fillMaxSize(),
            arrangementSpace = arrangementSpace,
            keyTextSize = keyTextSize,
            keyGrid = keyGrid,
            onClick = onClick,
            locale = locale,
        )
    }
}

@Composable
private fun KeyBox(
    modifier: Modifier,
    arrangementSpace: Dp,
    keyTextSize: TextUnit,
    keyGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(arrangementSpace)
    ) {
        keyGrid.forEach { keys ->
            KeyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                arrangementSpace = arrangementSpace,
                keyTextSize = keyTextSize,
                keys = keys,
                onClick = onClick,
                locale = locale,
            )
        }
    }
}

@Composable
private fun KeyRow(
    modifier: Modifier,
    arrangementSpace: Dp,
    keyTextSize: TextUnit,
    keys: List<Key?>,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(arrangementSpace),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        keys.forEach { key ->
            KeyButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                keyTextSize = keyTextSize,
                key = key,
                onClick = onClick,
                locale = locale,
            )
        }
    }
}

@Composable
private fun KeyButton(
    modifier: Modifier,
    keyTextSize: TextUnit,
    key: Key?,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (key != null) {
            IconButton(
                modifier = Modifier.fillMaxSize(),
                colors = key.colors(),
                onClick = { onClick(key) },
                onLongClick = { key.longClickKey?.let { onClick(it) } },
            ) {
                when (val display: Display = key.display(locale)) {
                    is Display.Text -> {
                        Text(
                            text = display.text,
                            fontSize = keyTextSize,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is Display.Drawable -> {
                        Icon(
                            modifier = Modifier.padding(6.dp),
                            painter = painterResource(display.id),
                            contentDescription = "",
                            tint = key.colors().contentColor,
                        )
                    }
                }
            }
        }
    }
}

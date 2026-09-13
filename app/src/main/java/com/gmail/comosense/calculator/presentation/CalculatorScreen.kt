package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Locale

@Composable
fun CalculatorScreen(
    appState: AppState,
    onClick: (Key) -> Unit,
    onShowHistory: () -> Unit,
) {
    val locale: Locale = LocalLocale.current.platformLocale

    AppScaffold {
        ScreenScaffold { _ ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) {
                val diameter = minOf(maxWidth, maxHeight) * 0.7071f
                Box(
                    modifier = Modifier
                        .size(diameter),
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
    val expressionTextSize: TextAutoSize = TextAutoSize.StepBased(
        maxFontSize = 64.sp,
        minFontSize = 12.sp,
        stepSize = 2.sp,
    )
    val expressionTextStyle: TextStyle = TextStyle(
        color = MaterialTheme.colorScheme.onBackground,
    )
    val keyTextSize: TextUnit = 18.sp
    val deleteKey: CommandKey = if (appState.isEntering) {
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
        Expression(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            appState = appState,
            expressionTextSize = expressionTextSize,
            expressionTextStyle = expressionTextStyle,
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

    androidx.compose.animation.AnimatedVisibility(
        visible = showOperatorKeyBox,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.tertiaryDim.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { showOperatorKeyBox = false },
        )
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = showOperatorKeyBox,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        enter = fadeIn() + scaleIn(initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
    ) {
        OperatorKeyBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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

@Composable
private fun Expression(
    modifier: Modifier,
    appState: AppState,
    expressionTextSize: TextAutoSize,
    expressionTextStyle: TextStyle,
    onShowHistory: () -> Unit,
    locale: Locale,
) {
    Box(
        modifier = modifier
            .clickable { onShowHistory() },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            modifier = Modifier,
            text = appState.displayExpression(locale),
            autoSize = expressionTextSize,
            style = expressionTextStyle,
        )
    }
}

@Composable
private fun OperatorKeyBox(
    modifier: Modifier,
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
    keyTextSize: TextUnit,
    keyGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyGrid.forEach { keys ->
            KeyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
    keyTextSize: TextUnit,
    keys: List<Key?>,
    onClick: (Key) -> Unit,
    locale: Locale,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                val text: String? = key.displayText(locale)
                if (text != null) {
                    Text(
                        text = text,
                        fontSize = keyTextSize,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    val drawable: Int? = key.drawable()
                    if (drawable != null) {
                        Icon(
                            modifier = Modifier.padding(6.dp),
                            painter = painterResource(drawable),
                            contentDescription = "",
                            tint = key.colors().contentColor,
                        )
                    }
                }
            }
        }
    }
}

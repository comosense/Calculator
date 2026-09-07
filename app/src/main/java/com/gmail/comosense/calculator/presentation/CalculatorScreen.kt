package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun CalculatorScreen(
    appState: AppState,
    onClick: (Key) -> Unit,
    onLongClick: (Key?) -> Unit,
    onShowHistory: () -> Unit
) {
    val expressionMaxFontSize: TextUnit = 36.sp
    val expressionMinFontSize: TextUnit = 12.sp
    val expressionFontStepSize: TextUnit = 2.sp
    val keyFontSize: TextUnit = 16.sp
    val containerColor: Color = MaterialTheme.colorScheme.background
    val contentColor: Color = MaterialTheme.colorScheme.onBackground
    val surfaceColor: Color = MaterialTheme.colorScheme.surfaceContainer
    val historyButtonContainerColor: Color = MaterialTheme.colorScheme.primaryContainer
    val historyButtonContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
    val keyChangeButtonContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer
    val keyChangeButtonContentColor: Color = MaterialTheme.colorScheme.onSurface

    val historyButtonAlpha = remember { Animatable(1f) }
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
    val parenthesisKey: SymbolKey =
        if (appState.canAppend(Symbol.FactorEnd.CloseParenthesis)) {
            SymbolKey.CloseParenthesis
        } else {
            SymbolKey.OpenParenthesis
        }
    val deleteKey: CommandKey = if (appState.isEntering) {
        CommandKey.Delete
    } else {
        CommandKey.Clear
    }
    val keyGrid = listOf(
        listOf(
            SymbolKey.Digit(7),
            SymbolKey.Digit(8),
            SymbolKey.Digit(9),
            deleteKey,
            CommandKey.Equals,
        ),
        listOf(
            SymbolKey.Digit(4),
            SymbolKey.Digit(5),
            SymbolKey.Digit(6),
            plusKey,
            minusKey,
        ),
        listOf(
            SymbolKey.Digit(1),
            SymbolKey.Digit(2),
            SymbolKey.Digit(3),
            SymbolKey.Multiply,
            SymbolKey.Divide,
        ),
        listOf(
            null,
            SymbolKey.Point,
            SymbolKey.Digit(0),
            parenthesisKey,
            null,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LaunchedEffect(appState.history) {
            repeat(2) {
                historyButtonAlpha.animateTo(
                    targetValue = 0.3f,
                    animationSpec = tween(150),
                )
                historyButtonAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(150),
                )
            }
        }
        Button(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .alpha(historyButtonAlpha.value),
            colors = ButtonDefaults.buttonColors(
                containerColor = historyButtonContainerColor,
                contentColor = historyButtonContentColor,
            ),
            onClick = onShowHistory,
        ) {}

        BasicText(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
                .background(containerColor)
                .padding(horizontal = 48.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            text = appState.displayExpression,
            autoSize = TextAutoSize.StepBased(
                minFontSize = expressionMinFontSize,
                maxFontSize = expressionMaxFontSize,
                stepSize = expressionFontStepSize,
            ),
            style = TextStyle(
                color = contentColor,
                textAlign = TextAlign.Center,
            ),
        )

        KeyGrid(
            modifier = Modifier
                .weight(16f)
                .fillMaxWidth(),
            fontSize = keyFontSize,
            keyGrid = keyGrid,
            onClick = onClick,
            onLongClick = onLongClick,
        )

        Button(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = keyChangeButtonContainerColor,
                contentColor = keyChangeButtonContentColor,
            ),
            onClick = { /* TODO: change the KeyGrid to function-keys */ },
        ) {}
    }
}

@Composable
fun KeyGrid(
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    keyGrid: List<List<Key?>>,
    onClick: (Key) -> Unit,
    onLongClick: (Key?) -> Unit,
) {
    Column(modifier = modifier) {
        keyGrid.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key != null) {
                            Button(
                                modifier = Modifier.fillMaxSize(),
                                colors = key.buttonColors(),
                                onClick = { onClick(key) },
                                onLongClick = { onLongClick(key.longClickKey) }
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = key.text,
                                    fontSize = fontSize,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

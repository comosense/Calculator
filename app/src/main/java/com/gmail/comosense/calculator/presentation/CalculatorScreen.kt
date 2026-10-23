package com.gmail.comosense.calculator.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.LocalTextStyle
import androidx.wear.compose.material3.ScreenScaffold
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.AppTheme

// Ratios derived from the geometry of the display area and panels.
// See doc/calculator-screen-constants.md for the derivation.
private const val INSCRIBED_LENGTH_RATIO: Float = 0.70710677f
private const val PANEL_WIDTH_RATIO: Float = 0.9055385f
private const val KEY_SWITCHER_LENGTH_RATIO: Float = 0.14142136f

@Composable
fun CalculatorScreen(
    appState: AppState,
    onAppAction: (AppAction) -> Unit,
    onShow: (AppScreen) -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    ScreenScaffold {
        MainPanel(
            modifier = Modifier.fillMaxSize(),
            appState = appState,
            onAppAction = onAppAction,
            onShow = onShow,
            symbolFormatter = symbolFormatter,
        )
    }
}

@Composable
private fun MainPanel(
    modifier: Modifier,
    appState: AppState,
    onAppAction: (AppAction) -> Unit,
    onShow: (AppScreen) -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    var showFunctionKeyGrid by remember { mutableStateOf(false) }
    var isInverseFunctionMode by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.background(AppTheme.calculatorScreenColors.background),
        contentAlignment = Alignment.Center,
    ) {
        val displayDiameter: Dp = minOf(maxWidth, maxHeight)
        val inscribedLength: Dp = displayDiameter * INSCRIBED_LENGTH_RATIO
        val panelWidth: Dp = displayDiameter * PANEL_WIDTH_RATIO
        val keySwitcherLength: Dp = displayDiameter * KEY_SWITCHER_LENGTH_RATIO

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(
                modifier = Modifier.size(keySwitcherLength)
            )

            ExpressionBox(
                modifier = Modifier.size(
                    width = inscribedLength,
                    height = inscribedLength / 5f
                ),
                appState = appState,
                onAppAction = onAppAction,
                onShow = onShow,
                symbolFormatter = symbolFormatter,
            )

            AnimatedContent(
                targetState = showFunctionKeyGrid,
                transitionSpec = {
                    slideInVertically(
                        animationSpec = tween(250),
                        initialOffsetY = { height -> height },
                    ) togetherWith slideOutVertically(
                        animationSpec = tween(250),
                        targetOffsetY = { height -> height }
                    )
                },
                modifier = Modifier.size(
                    width = panelWidth,
                    height = inscribedLength * (4f / 5f)
                ),

                ) { isShowFunctionKeyGrid ->
                KeyGrid(
                    keyGrid = if (isShowFunctionKeyGrid) {
                        functionKeyGrid(
                            isInverseFunctionMode = isInverseFunctionMode,
                            appState = appState,
                        )
                    } else {
                        mainKeyGrid(
                            appState = appState,
                        )
                    },
                    arrangementSpace = 2.dp,
                    modifier = Modifier.fillMaxSize(),
                    onAction = { action ->
                        when (action) {
                            is Action.App -> onAppAction(action.appAction)
                            is Action.Ui -> when (action.uiAction) {
                                UiAction.ToggleFunctionMode ->
                                    isInverseFunctionMode = !isInverseFunctionMode
                            }
                        }
                    },
                    symbolFormatter = symbolFormatter,
                )
            }

            IconButton(
                onClick = { showFunctionKeyGrid = !showFunctionKeyGrid },
                modifier = Modifier.size(keySwitcherLength)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_key_switcher),
                    contentDescription = stringResource(R.string.content_description_key_switcher),
                    modifier = Modifier.fillMaxSize(),
                    tint = AppTheme.calculatorScreenColors.keySwitcher,
                )
            }
        }
    }
}

@Composable
private fun ExpressionBox(
    modifier: Modifier,
    appState: AppState,
    onAppAction: (AppAction) -> Unit,
    onShow: (AppScreen) -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    val scrollState: ScrollState = rememberScrollState()
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val blinkProgress: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    val blinkColor: Color = AppTheme.calculatorScreenColors.expressionBlinkBackground
    val expressionColor: Color = if (appState.isCalculating) {
        AppTheme.calculatorScreenColors.expression.copy(alpha = 0.375f)
    } else {
        AppTheme.calculatorScreenColors.expression
    }
    val expression: AnnotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = expressionColor)) {
            append(symbolFormatter.format(appState.fixedExpression))
        }
        withStyle(style = SpanStyle(color = expressionColor.copy(alpha = 0.375f))) {
            append(symbolFormatter.format(appState.completionInput))
        }
    }.ifEmpty {
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = expressionColor)) {
                append("0")
            }
        }
    }

    LaunchedEffect(appState.histories) {
        repeat(2) {
            blinkProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 180,
                    easing = FastOutSlowInEasing,
                ),
            )
            blinkProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .combinedClickable(
                onClick = { onShow(AppScreen.History) },
                onLongClick = { onShow(AppScreen.Settings) },
            )
            .drawBehind {
                if (blinkProgress.value > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                blinkColor.copy(alpha = 0.375f * blinkProgress.value),
                                blinkColor.copy(alpha = 0.25f * blinkProgress.value),
                                blinkColor.copy(alpha = 0.125f * blinkProgress.value),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.maxDimension * 0.5f,
                        ),
                        cornerRadius = CornerRadius(
                            x = size.minDimension * 0.5f,
                            y = size.minDimension * 0.5f,
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val density: Density = LocalDensity.current
        val minFontSize: TextUnit = AppTheme.calculatorScreenDimensions.expressionMinFontSize
        val style: TextStyle = LocalTextStyle.current.copy(fontSize = minFontSize)
        val measured: TextLayoutResult = remember(
            expression,
            minFontSize,
            maxWidth,
        ) {
            textMeasurer.measure(
                text = expression,
                style = style
            )
        }

        val needScroll: Boolean = with(density) {
            measured.size.width > maxWidth.toPx()
        }

        LaunchedEffect(
            expression,
            needScroll,
            scrollState.maxValue,
            appState.isInputting,
        ) {
            if (needScroll) {
                scrollState.scrollTo(
                    if (appState.isInputting) {
                        scrollState.maxValue
                    } else {
                        0
                    }
                )
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
                    color = expressionColor,
                    fontSize = AppTheme.calculatorScreenDimensions.expressionMinFontSize,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = expression,
                color = expressionColor,
                autoSize = TextAutoSize.StepBased(
                    maxFontSize = AppTheme.calculatorScreenDimensions.expressionMaxFontSize,
                    minFontSize = AppTheme.calculatorScreenDimensions.expressionMinFontSize,
                ),
                maxLines = 1,
            )
        }

        if (appState.isCalculating) {
            val size: Dp = maxHeight * 0.75f
            IconButton(
                onClick = { onAppAction(AppAction.CalculateCancel) },
                modifier = Modifier.size(size),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = stringResource(R.string.content_description_cancel),
                    modifier = Modifier.fillMaxSize(),
                    tint = AppTheme.calculatorScreenColors.cancelButton,
                )
            }
            CircularProgressIndicator(
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
private fun KeyGrid(
    keyGrid: List<List<Key?>>,
    arrangementSpace: Dp,
    modifier: Modifier,
    onAction: (Action) -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(arrangementSpace)
    ) {
        keyGrid.forEach { keys ->
            KeysRow(
                keys = keys,
                arrangementSpace = arrangementSpace,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onAction = onAction,
                symbolFormatter = symbolFormatter,
            )
        }
    }
}

@Composable
private fun KeysRow(
    keys: List<Key?>,
    arrangementSpace: Dp,
    modifier: Modifier,
    onAction: (Action) -> Unit,
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
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    onAction = onAction,
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
    modifier: Modifier,
    onAction: (Action) -> Unit,
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

        Style.Mode ->
            Colors(
                container = AppTheme.calculatorScreenColors.modeKeyContainer,
                content = AppTheme.calculatorScreenColors.modeKeyContent,
            )
    }

    Button(
        onClick = { onAction(key.action) },
        onLongClick = {
            key.altKeyOrNull?.let { altKey ->
                onAction(altKey.action)
            }
        },
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
                is Display.Text ->
                    Text(
                        text = display.text,
                        color = colors.content,
                        fontSize = AppTheme.calculatorScreenDimensions.keyTextFontSize,
                        textAlign = TextAlign.Center,
                    )

                is Display.Icon ->
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

private fun mainKeyGrid(
    appState: AppState
): List<List<Key?>> {
    val deleteKey: Key =
        if (appState.isInputting) {
            Key.Backspace
        } else {
            Key.Clear
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
    val parenthesisKey: Key =
        if (appState.canAppend(Symbol.FactorEnd.ClosingParenthesis)) {
            Key.ClosingParenthesis
        } else {
            Key.OpeningParenthesis
        }

    return listOf(
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
}

private fun functionKeyGrid(
    isInverseFunctionMode: Boolean,
    appState: AppState,
): List<List<Key?>> {
    return listOf(
        listOf(
            Key.Sqrt,
            Key.Power,
            Key.Factorial,
        ),
        listOf(
            Key.Log,
            Key.Ln,
            Key.Euler,
        ),
        if (isInverseFunctionMode) {
            listOf(
                Key.Arcsin,
                Key.Arccos,
                Key.Arctan,
            )
        } else {
            listOf(
                Key.Sin,
                Key.Cos,
                Key.Tan,
            )
        },
        listOf(
            null,
            Key.AngleMode(appState.isRadians),
            Key.Pi,
            Key.FunctionMode(isInverseFunctionMode),
            null,
        ),
    )
}

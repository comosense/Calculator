package com.gmail.comosense.calculator.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.data.History
import com.gmail.comosense.calculator.presentation.theme.AppTheme

data class HistoryItemContent(
    val containerColor: Color,
    val contentColor: Color,
    val expressionColor: Color,
    val resultColor: Color,
    val icon: (@Composable BoxScope.() -> Unit)? = null,
)

@Composable
fun HistoryScreen(
    histories: List<History>,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
    onDeleteModeChanged: (Boolean) -> Unit,
    symbolFormatter: SymbolFormatter,
) {
    var deleteMode: Boolean by remember { mutableStateOf(false) }
    val listState: TransformingLazyColumnState = rememberTransformingLazyColumnState()
    val transformationSpec: TransformationSpec = rememberTransformationSpec()

    LaunchedEffect(deleteMode) {
        onDeleteModeChanged(deleteMode)
    }

    BackHandler {
        if (deleteMode) {
            deleteMode = false
        } else {
            onBack()
        }
    }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            if (deleteMode) {
                EdgeButton(
                    onClick = {
                        deleteMode = false
                        onAction(AppAction.DeleteHistoryAll)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.historyScreenColors.deleteAllButtonContainer,
                        contentColor = AppTheme.historyScreenColors.deleteAllButtonContent,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.delete_all),
                        fontSize = AppTheme.historyScreenDimensions.buttonFontSize,
                    )
                }
            } else {
                EdgeButton(
                    onClick = {
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.historyScreenColors.backButtonContainer,
                        contentColor = AppTheme.historyScreenColors.backButtonContent,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_undo),
                        contentDescription = stringResource(R.string.content_description_undo),
                        tint = AppTheme.historyScreenColors.backButtonContent,
                    )
                }
            }
        },
    ) { contentPadding ->
        HistoryList(
            histories = histories,
            historyItemContent = if (deleteMode) {
                HistoryItemContent(
                    containerColor = AppTheme.historyScreenColors.deleteContainer,
                    contentColor = AppTheme.historyScreenColors.deleteContent,
                    expressionColor = AppTheme.historyScreenColors.deleteExpression,
                    resultColor = AppTheme.historyScreenColors.deleteResult,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.content_description_delete),
                            modifier = Modifier.align(Alignment.Center),
                            tint = AppTheme.historyScreenColors.deleteIcon,
                        )
                    }
                )
            } else {
                HistoryItemContent(
                    containerColor = AppTheme.historyScreenColors.historyContainer,
                    contentColor = AppTheme.historyScreenColors.historyContent,
                    expressionColor = AppTheme.historyScreenColors.historyExpression,
                    resultColor = AppTheme.historyScreenColors.historyResult,
                )
            },
            onClick = { id ->
                if (deleteMode) {
                    onAction(AppAction.DeleteHistory(id))
                    if (histories.size == 1) {
                        deleteMode = false
                    }
                } else {
                    onAction(AppAction.SelectHistory(id))
                    onBack()
                }
            },
            onLongClick = {
                deleteMode = !deleteMode
            },
            listState = listState,
            transformationSpec = transformationSpec,
            contentPadding = contentPadding,
            symbolFormatter = symbolFormatter,
        )
    }
}

@Composable
private fun HistoryList(
    histories: List<History>,
    historyItemContent: HistoryItemContent,
    onClick: (String) -> Unit,
    onLongClick: () -> Unit,
    listState: TransformingLazyColumnState,
    transformationSpec: TransformationSpec,
    contentPadding: PaddingValues,
    symbolFormatter: SymbolFormatter,
) {
    TransformingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.historyScreenColors.background),
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 48.dp,
            bottom = contentPadding.calculateBottomPadding() + 48.dp,
        ),
    ) {
        if (histories.isNotEmpty()) {
            items(
                count = histories.size,
                key = { index -> histories[index].id },
            ) { index ->
                val history: History = histories[index]

                HistoryItem(
                    history = history,
                    historyItemContent = historyItemContent,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .padding(horizontal = 8.dp),
                    transformation = SurfaceTransformation(transformationSpec),
                    symbolFormatter = symbolFormatter,
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.no_history),
                        color = AppTheme.historyScreenColors.textContent,
                        fontSize = AppTheme.historyScreenDimensions.textFontSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    history: History,
    historyItemContent: HistoryItemContent,
    onClick: (String) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
    symbolFormatter: SymbolFormatter,
) {
    val expressionScrollState: ScrollState = rememberScrollState()
    val resultScrollState: ScrollState = rememberScrollState()

    Button(
        onClick = { onClick(history.id) },
        onLongClick = onLongClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = historyItemContent.containerColor,
            contentColor = historyItemContent.contentColor,
        ),
        transformation = transformation,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            historyItemContent.icon?.invoke(this)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = symbolFormatter.format(history.expression),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(expressionScrollState),
                    color = historyItemContent.expressionColor,
                    fontSize = AppTheme.historyScreenDimensions.expressionFontSize,
                    softWrap = false,
                    maxLines = 1,
                )

                Text(
                    text = symbolFormatter.format(history.result),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(resultScrollState),
                    color = historyItemContent.resultColor,
                    fontSize = AppTheme.historyScreenDimensions.resultFontSize,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    maxLines = 1,
                )
            }
        }
    }
}

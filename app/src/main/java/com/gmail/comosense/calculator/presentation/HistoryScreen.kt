package com.gmail.comosense.calculator.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.gmail.comosense.calculator.R
import com.gmail.comosense.calculator.domain.Calculation
import com.gmail.comosense.calculator.domain.Symbol
import com.gmail.comosense.calculator.presentation.theme.CalculatorTheme
import com.gmail.comosense.calculator.presentation.theme.HistoryScreenColors
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<Calculation>,
    onAction: (AppAction) -> Unit,
    onBack: () -> Unit,
    locale: Locale,
) {
    var deleteMode: Boolean by remember { mutableStateOf(false) }
    val listState: TransformingLazyColumnState = rememberTransformingLazyColumnState()
    val transformationSpec: TransformationSpec = rememberTransformationSpec()

    BackHandler {
        if (deleteMode) {
            deleteMode = false
        } else {
            onBack()
        }
    }

    AppScaffold {
        ScreenScaffold(scrollState = listState) { contentPadding ->
            HistoryList(
                history = history,
                deleteMode = deleteMode,
                onClick = { result ->
                    onAction(AppAction.SelectHistory(result))
                    onBack()
                },
                onLongCLick = { deleteMode = true },
                onDelete = { index ->
                    onAction(AppAction.DeleteHistory(index))
                    if (history.size == 1) {
                        onBack()
                    }
                },
                onDeleteAll = {
                    onAction(AppAction.DeleteHistoryAll)
                    onBack()
                },
                listState = listState,
                transformationSpec = transformationSpec,
                contentPadding = contentPadding,
                locale = locale,
            )
        }
    }
}


@Composable
private fun HistoryList(
    history: List<Calculation>,
    deleteMode: Boolean,
    onClick: (List<Symbol>) -> Unit,
    onLongCLick: () -> Unit,
    onDelete: (Int) -> Unit,
    onDeleteAll: () -> Unit,
    listState: TransformingLazyColumnState,
    transformationSpec: TransformationSpec,
    contentPadding: PaddingValues,
    locale: Locale,
) {
    TransformingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 48.dp,
            bottom = contentPadding.calculateBottomPadding() + 48.dp,
        ),
    ) {
        items(
            count = history.size,
            key = { index -> index },
        ) { index ->
            val calculation: Calculation = history[index]

            HistoryItem(
                calculation = calculation,
                deleteMode = deleteMode,
                onClick = {
                    if (deleteMode) {
                        onDelete(index)
                    } else {
                        onClick(calculation.result)
                    }
                },
                onLongClick = onLongCLick,
                modifier = Modifier
                    .fillMaxWidth()
                    .transformedHeight(this, transformationSpec)
                    .padding(horizontal = 8.dp),
                transformation = SurfaceTransformation(transformationSpec),
                locale = locale
            )
        }
        if (deleteMode) {
            item {
                DeleteAllButton(onClick = onDeleteAll)
            }
        }
    }
}

@Composable
fun HistoryItem(
    calculation: Calculation,
    deleteMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    transformation: SurfaceTransformation,
    locale: Locale,
) {
    val expressionFontSize: TextUnit = 16.sp
    val resultFontSize: TextUnit = 18.sp
    val colors: HistoryScreenColors = CalculatorTheme.historyScreenColors

    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (deleteMode) {
                colors.deleteContainer
            } else {
                colors.container
            },
            contentColor = if (deleteMode) {
                colors.deleteContent
            } else {
                colors.content
            },
        ),
        transformation = transformation,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (deleteMode) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.align(Alignment.Center),
                    tint = colors.deleteIcon,
                )
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val expressionScrollState = rememberScrollState()
                val resultScrollState = rememberScrollState()

                Text(
                    text = calculation.expression.formatSymbols(locale),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(expressionScrollState),
                    fontSize = expressionFontSize,
                    softWrap = false,
                    maxLines = 1,
                )

                Text(
                    text = calculation.result.formatSymbols(locale),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(resultScrollState),
                    fontSize = resultFontSize,
                    textAlign = TextAlign.End,
                    softWrap = false,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DeleteAllButton(
    onClick: () -> Unit,
) {
    val colors: HistoryScreenColors = CalculatorTheme.historyScreenColors

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.deleteAllContainer,
            contentColor = colors.deleteAllContent,
        )
    ) {
        Text(stringResource(R.string.delete_all))
    }
}

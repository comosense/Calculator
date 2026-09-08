package com.gmail.comosense.calculator.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<Calculation>,
    onHistoryClick: (List<Symbol>) -> Unit,
    onHistoryDelete: (Int) -> Unit,
    onHistoryDeleteAll: () -> Unit,
    onBack: () -> Unit,
) {
    var deleteMode: Boolean by remember { mutableStateOf(false) }
    val listState: TransformingLazyColumnState = rememberTransformingLazyColumnState()
    val transformationSpec: TransformationSpec = rememberTransformationSpec()

    val locale: Locale = LocalLocale.current.platformLocale
    val expressionFontSize: TextUnit = 16.sp
    val resultFontSize: TextUnit = 18.sp
    val backgroundColor: Color = MaterialTheme.colorScheme.background
    val errorContainerColor: Color = MaterialTheme.colorScheme.errorContainer
    val errorContentColor: Color = MaterialTheme.colorScheme.onErrorContainer
    val edgeButtonContainerColor: Color = MaterialTheme.colorScheme.primaryContainer
    val edgeButtonContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
    val historyContainerColor: Color = MaterialTheme.colorScheme.background
    val historyContentColor: Color = MaterialTheme.colorScheme.onBackground
    val deleteHistoryContainerColor: Color = MaterialTheme.colorScheme.tertiaryContainer
    val deleteHistoryContentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer
    val edgeButton: @Composable BoxScope. () -> Unit = {
        EdgeButton(
            onClick = {
                if (deleteMode) {
                    deleteMode = false
                } else {
                    onBack()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = edgeButtonContainerColor,
                contentColor = edgeButtonContentColor,
            ),
        ) { Text(if (deleteMode) "Cancel" else "Back") }
    }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.BottomCenter,
        ) { edgeButton() }
    } else {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = edgeButton,
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                TransformingLazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = contentPadding,
                    state = listState,
                ) {
                    items(
                        count = history.size,
                        key = { index -> index },
                    ) { index ->
                        val calculation = history[index]

                        Button(
                            onClick = {
                                if (deleteMode) {
                                    onHistoryDelete(index)
                                    if (history.size == 1) {
                                        deleteMode = false
                                    }
                                } else {
                                    onHistoryClick(calculation.result)
                                    onBack()
                                }
                            },
                            onLongClick = {
                                deleteMode = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(horizontal = 8.dp),
                            transformation = SurfaceTransformation(transformationSpec),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (deleteMode) {
                                    deleteHistoryContainerColor
                                } else {
                                    historyContainerColor
                                },
                                contentColor = if (deleteMode) {
                                    deleteHistoryContentColor
                                } else {
                                    historyContentColor
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val expressionScrollState = rememberScrollState()
                                val resultScrollState = rememberScrollState()

                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(expressionScrollState),
                                    text = calculation.expression.formatSymbols(locale),
                                    fontSize = expressionFontSize,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(resultScrollState),
                                    text = calculation.result.formatSymbols(locale),
                                    fontSize = resultFontSize,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                    if (deleteMode) {
                        item {
                            Button(
                                onClick = {
                                    onHistoryDeleteAll()
                                    deleteMode = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = errorContainerColor,
                                    contentColor = errorContentColor,
                                )
                            ) {
                                Text("Delete All")
                            }
                        }
                    }
                }
            }
        }
    }
}

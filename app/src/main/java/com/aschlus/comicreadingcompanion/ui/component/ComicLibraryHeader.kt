package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

private val LibraryHeaderContentHeight = 136.dp

@Composable
fun ComicLibraryHeader(
    onCreateReadingListClick: () -> Unit,
    onImportReadingListClick: () -> Unit,
    isImporting: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(ComicBlue)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start =
                            Offset(
                                x = 0f,
                                y = size.height - strokeWidth / 2f
                            ),
                        end =
                            Offset(
                                x = size.width,
                                y = size.height - strokeWidth / 2f
                            ),
                        strokeWidth = strokeWidth
                    )
                }
    ) {
        ComicHalftoneOverlay(
            modifier = Modifier.matchParentSize(),
            dotColor = ComicInk,
            spacing = 10.dp,
            radius = 1.1.dp,
            startFraction = 0f,
            minAlpha = 0.3f,
            maxAlpha = 0.14f
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {

            Spacer(
                modifier =
                    Modifier.windowInsetsTopHeight(
                        WindowInsets.statusBars
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            LibraryHeaderContentHeight
                        )
            ) {
                LibraryTitle(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .padding(
                                start = 25.dp
                            )
                )

                Column(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .padding(
                                end = 16.dp
                            )
                            .width(142.dp)
                ) {
                    ComicLibraryActionButton(
                        text = "CREATE\nLIST",
                        icon = Icons.Default.Add,
                        backgroundColor = ComicYellow,
                        onClick = onCreateReadingListClick
                    )

                    ComicLibraryActionButton(
                        text =
                            if (isImporting) {
                                "IMPORTING…"
                            } else {
                                "IMPORT\nLIST"
                            },
                        icon = Icons.Default.Upload,
                        backgroundColor = ComicBlueLight,
                        onClick = onImportReadingListClick,
                        modifier =
                            Modifier.padding(
                                top = 7.dp
                            ),
                        enabled = !isImporting
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryTitle(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val outlineWidth =
        with (density) {
            1.8.dp.toPx()
        }

    val titleStyle = MaterialTheme.typography.displayLarge
        .copy(
            fontSize = 54.sp,
            lineHeight = 54.sp,
            letterSpacing = (-2).sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic
        )

    Box(
        modifier = modifier
    ) {
        // Deep comic-book extrusion

        DecorativeLibraryTitle(
            modifier =
                Modifier.offset(
                    x = 5.dp,
                    y = 7.dp
                ),
            style = titleStyle,
            color = ComicInk
        )

        // Continuous black outline
        DecorativeLibraryTitle(
            style =
                titleStyle.copy(
                    drawStyle =
                        Stroke(
                            width = outlineWidth
                        )
                ),
            color = ComicInk
        )

        // Yellow face
        Text(
            text = "LIBRARY",
            style = titleStyle,
            color = ComicYellow
        )
    }
}

@Composable
private fun DecorativeLibraryTitle(
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = "LIBRARY",
        modifier =
            modifier
                .clearAndSetSemantics { },
        style = style,
        color = color
    )
}
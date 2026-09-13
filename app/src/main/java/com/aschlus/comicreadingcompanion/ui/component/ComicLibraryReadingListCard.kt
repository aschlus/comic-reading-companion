package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import kotlin.math.abs
import kotlin.math.roundToInt

private val LibraryCardBackground =
    Color(0xFFFFFCF5)

private val LibraryCardShape =
    RoundedCornerShape(14.dp)

@Composable
fun ComicLibraryReadingListCard(
    title: String,
    description: String?,
    readCount: Int,
    totalCount: Int,
    progress: Float,
    continueText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionPercent =
        (progress.coerceIn(0f, 1f) * 100f)
            .roundToInt()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 4.dp
                )
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 5.dp,
                        y = 5.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = LibraryCardShape
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 120.dp
                    )
                    .background(
                        color = LibraryCardBackground,
                        shape = LibraryCardShape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = LibraryCardShape
                    )
                    .clickable(
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 8.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicReadingListPlaceholder(
                title = title
            )

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                            .copy(
                                fontSize = 17.sp,
                                lineHeight = 20.sp
                            ),
                    fontWeight =
                        FontWeight.Black,
                    color = ComicInk,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis
                )

                description?.let {
                    Text(
                        text = it,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                                .copy(
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp
                                ),
                        color = ComicInk,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Text(
                    text =
                        "$readCount of $totalCount read" +
                                " • " +
                                "$completionPercent% complete",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            ),
                    fontWeight =
                        FontWeight.Bold,
                    color = ComicInk,
                    maxLines = 1
                )

                ComicProgressBar(
                    progress = progress,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (continueText != null) {
                    Text(
                        text =
                            "Next up: $continueText",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                                .copy(
                                    fontSize = 14.sp,
                                    lineHeight = 17.sp
                                ),
                        fontWeight =
                            FontWeight.Bold,
                        color = ComicInk,
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }

            ComicReadingListChevron()
        }
    }
}

@Composable
private fun ComicReadingListChevron() {
    Canvas(
        modifier =
            Modifier.size(
                width = 16.dp,
                height = 30.dp
            )
    ) {
        val strokeWidth =
            3.5.dp.toPx()

        drawLine(
            color = ComicInk,
            start =
                Offset(
                    x = size.width * 0.25f,
                    y = size.height * 0.18f
                ),
            end =
                Offset(
                    x = size.width * 0.76f,
                    y = size.height * 0.50f
                ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )

        drawLine(
            color = ComicInk,
            start =
                Offset(
                    x = size.width * 0.76f,
                    y = size.height * 0.50f
                ),
            end =
                Offset(
                    x = size.width * 0.25f,
                    y = size.height * 0.82f
                ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square
        )
    }
}

@Composable
private fun ComicReadingListPlaceholder(
    title: String
) {
    val colors =
        listOf(
            ComicBlue,
            ComicRed,
            ComicYellow
        )

    val backgroundColor =
        colors[
            abs(
                title.hashCode()
            ) % colors.size
        ]

    Box(
        modifier =
            Modifier
                .size(
                    width = 74.dp,
                    height = 104.dp
                )
                .background(
                    backgroundColor
                )
                .border(
                    width = 2.dp,
                    color = ComicInk
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Canvas(
            modifier =
                Modifier.matchParentSize()
        ) {
            val path =
                Path().apply {
                    moveTo(
                        size.width * 0.5f,
                        0f
                    )

                    lineTo(
                        size.width * 0.62f,
                        size.height * 0.34f
                    )

                    lineTo(
                        size.width,
                        size.height * 0.5f
                    )

                    lineTo(
                        size.width * 0.62f,
                        size.height * 0.66f
                    )

                    lineTo(
                        size.width * 0.5f,
                        size.height
                    )

                    lineTo(
                        size.width * 0.38f,
                        size.height * 0.66f
                    )

                    lineTo(
                        0f,
                        size.height * 0.5f
                    )

                    lineTo(
                        size.width * 0.38f,
                        size.height * 0.34f
                    )

                    close()
                }

            drawPath(
                path = path,
                color =
                    ComicPaper.copy(
                        alpha = 0.45f
                    )
            )

            drawCircle(
                color =
                    ComicInk.copy(
                        alpha = 0.18f
                    ),
                radius =
                    size.minDimension * 0.12f,
                center =
                    Offset(
                        x = size.width * 0.25f,
                        y = size.height * 0.22f
                    )
            )
        }

        Text(
            text =
                title
                    .split(" ")
                    .filter {
                        it.isNotBlank()
                    }
                    .take(2)
                    .joinToString("") {
                        it.take(1)
                    }
                    .uppercase(),
            style =
                MaterialTheme
                    .typography
                    .headlineMedium,
            fontWeight =
                FontWeight.Black,
            color = ComicInk
        )
    }
}
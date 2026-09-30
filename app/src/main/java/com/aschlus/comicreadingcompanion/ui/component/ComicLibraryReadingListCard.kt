package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import kotlin.math.roundToInt

@Composable
fun ComicLibraryReadingListCard(
    title: String,
    description: String?,
    readCount: Int,
    totalCount: Int,
    progress: Float,
    continueText: String?,
    artworkSeed: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ReadingListStyle = ReadingListStyle.GREEN
) {
    val shape = RoundedCornerShape(14.dp)

    val completionPercent =
        (progress.coerceIn(0f, 1f) * 100).roundToInt()


    Box(
        modifier = modifier
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = 120.dp
                    )
                    .clip(shape)
                    .background(ComicPaper)
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            ComicReadingListArtwork(
                title = title,
                coverUrls = emptyList(),
                seed = artworkSeed,
                accentColor = readingListAccentColor(style),
                modifier =
                    Modifier.size(
                        width = 74.dp,
                        height = 104.dp
                    ),
                shape = RoundedCornerShape(4.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        lineHeight = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text =
                        "$readCount of $totalCount read • " +
                        "$completionPercent% complete",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                ComicProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )

                if (continueText != null) {
                    Text(
                        text =
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(
                                        "Next up: "
                                    )
                                }

                                append(continueText)
                            },
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Canvas(
                modifier =
                    Modifier.size(
                        width = 18.dp,
                        height = 30.dp
                    )
            ) {
                val strokeWidth = 3.dp.toPx()

                drawLine(
                    color = ComicInk,
                    start =
                        Offset(
                            x = size.width * 0.25f,
                            y = size.height * 0.18f
                        ),
                    end =
                        Offset(
                            x = size.width * 0.72f,
                            y = size.height * 0.50f
                        ),
                    strokeWidth = strokeWidth
                )

                drawLine(
                    color = ComicInk,
                    start =
                        Offset(
                            x = size.width * 0.72f,
                            y = size.height * 0.50f,
                        ),
                    end =
                        Offset(
                            x = size.width * 0.25f,
                            y = size.height * 0.82f
                        ),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
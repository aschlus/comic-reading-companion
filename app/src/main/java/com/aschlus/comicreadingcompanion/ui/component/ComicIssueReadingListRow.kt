package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.data.database.models.IssueReadingListResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicGray
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicOrange
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

@Composable
fun ComicIssueReadingListRow(
    readingList: IssueReadingListResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    val accentColor =
        readingListStyleColor(readingList.style)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(76.dp)
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
                    .height(72.dp)
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
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicReadingListBooksIcon(
                color = accentColor,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text = readingList.title,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 16.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = ComicInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "${readingList.readCount} " +
                        "of " +
                        "${readingList.totalCount} " +
                        "issues",
                    style =
                        MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            ),
                    color = ComicInk.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "›",
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ComicInk
            )
        }
    }
}

@Composable
private fun ComicReadingListBooksIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val strokeWidth  = 2.dp.toPx()
        val bookWidth = 34.dp.toPx()
        val bookHeight = 14.dp.toPx()

        val left = (size.width - bookWidth) / 2f

        fun drawBook(
            top: Float
        ) {
            val corner =
                CornerRadius(
                    x = 3.dp.toPx(),
                    y = 3.dp.toPx()
                )

            drawRoundRect(
                color = ComicInk,
                topLeft =
                    Offset(
                        x = left + 2.dp.toPx(),
                        y = top + 2.dp.toPx()
                    ),
                size =
                    Size(
                        width = bookWidth,
                        height = bookHeight
                    ),
                cornerRadius = corner
            )

            drawRoundRect(
                color = color,
                topLeft =
                    Offset(
                        x = left,
                        y = top
                    ),
                size =
                    Size(
                        width = bookWidth,
                        height = bookHeight
                    ),
                cornerRadius = corner
            )

            drawRoundRect(
                color = ComicInk,
                topLeft =
                    Offset(
                        x = left,
                        y = top
                    ),
                size =
                    Size(
                        width = bookWidth,
                        height = bookHeight
                    ),
                cornerRadius = corner,
                style =
                    Stroke(
                        width = strokeWidth
                    )
            )

            drawLine(
                color = ComicInk,
                start =
                    Offset(
                        x = left + 8.dp.toPx(),
                        y = top
                    ),
                end =
                    Offset(
                        x = left + 8.dp.toPx(),
                        y = top + bookHeight
                    ),
                strokeWidth = strokeWidth
            )
        }

        drawBook(top = 7.dp.toPx())
        drawBook(top = 27.dp.toPx())
    }
}

private fun readingListStyleColor(
    style: ReadingListStyle
): Color {
    return when (style) {
        ReadingListStyle.GREEN ->
            ComicGreen

        ReadingListStyle.RED ->
            ComicRed

        ReadingListStyle.BLUE ->
            ComicBlue

        ReadingListStyle.PURPLE ->
            ComicPurple

        ReadingListStyle.ORANGE ->
            ComicOrange

        ReadingListStyle.GRAY ->
            ComicGray
    }
}
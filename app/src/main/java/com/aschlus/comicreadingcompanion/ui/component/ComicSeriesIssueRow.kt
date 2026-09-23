package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.SeriesIssue
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ComicSeriesIssueRow(
    issue: SeriesIssue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.fillMaxWidth()
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
                    .clip(shape)
                    .background(
                        ComicPaper
                    )
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
                        vertical = 10.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicCoverImage(
                coverUrl = issue.coverUrl,
                contentDescription = "#${issue.issueNumber} cover",
                modifier =
                    Modifier
                        .width(64.dp)
                        .aspectRatio(2f / 3f),
                placeholderText = "No Cover",
                shape = RoundedCornerShape(4.dp),
                borderColor = ComicInk
            )

            Spacer(modifier = Modifier.width(11.dp))

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "#${issue.issueNumber}",
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 16.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = ComicInk
                )

                issue.issueTitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { title ->
                        Text(
                            text = title,
                            style =
                                MaterialTheme.typography.bodyMedium
                                    .copy(
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                            color = ComicInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                issue.publicationDate
                    ?.let { publicationDate ->
                        Text(
                            text = formatSeriesIssueDate(publicationDate),
                            style =
                                MaterialTheme.typography.bodySmall
                                    .copy(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    ),
                            color = ComicInk.copy(alpha = 0.72f)
                        )
                    }

                Text(
                    text =
                        issue.issueType
                            .name
                            .replace("_", " ")
                            .lowercase()
                            .replaceFirstChar {
                                it.titlecase(
                                    Locale.getDefault()
                                )
                            },
                    style =
                        MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            ),
                    color = ComicInk.copy(alpha = 0.72f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            ComicSeriesIssueStatus(
                readingStatus = issue.readingStatus
            )

            Spacer(modifier = Modifier.width(8.dp))

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
private fun ComicSeriesIssueStatus(
    readingStatus: ReadingStatus?
) {
    val label =
        when (readingStatus) {
            ReadingStatus.READ ->
                "READ"

            ReadingStatus.READING ->
                "READING"

            else ->
                "UNREAD"
        }

    val backgroundColor =
        when (readingStatus) {
            ReadingStatus.READ ->
                ComicBlueLight

            ReadingStatus.READING ->
                ComicYellow

            else ->
                ComicPaper
        }

    val shape =
        RoundedCornerShape(7.dp)

    Box(
        modifier =
            Modifier
                .clip(shape)
                .background(
                    backgroundColor
                )
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium
                    .copy(
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    ),
            color = ComicInk,
            maxLines = 1
        )
    }
}

private fun formatSeriesIssueDate(
    publicationDate: String
): String {
    return try {
        YearMonth
            .parse(
                publicationDate
            )
            .format(
                DateTimeFormatter
                    .ofPattern(
                        "MMM yyyy",
                        Locale.getDefault()
                    )
            )
    } catch (
        _: Exception
    ) {
        publicationDate
    }
}
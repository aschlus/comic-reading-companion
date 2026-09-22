package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

@Composable
fun ComicBrowseSeriesResult(
    result: SeriesSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metadata =
        buildList {
            result.volume?.let {
                add("Volume $it")
            }

            result.startYear?.let {
                add(it.toString())
            }

            add(result.publisherName)
        }.joinToString( " • ")

    ComicBrowseResultRow(
        badgeText =
            result.title
                .firstOrNull()
                ?.uppercaseChar()
                ?.toString()
                ?: "?",
        badgeColor = ComicPurple,
        title = result.title,
        subtitle = metadata,
        supportingText =
            "${result.readCount} of " +
            "${result.totalCount} read",
        statusText =
            if (
                result.totalCount > 0 &&
                result.readCount == result.totalCount
            ) {
                "READ"
            } else {
                null
            },
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ComicBrowseIssueResult(
    result: IssueSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metadata =
        buildList {
            result.seriesVolume?.let {
                add("Volume $it")
            }

            result.publicationDate?.let {
                add(it)
            }

            add(result.publisherName)
        }.joinToString( " • ")

    val statusText =
        when (result.readingStatus) {
            ReadingStatus.READ ->
                "READ"

            ReadingStatus.READING ->
                "READING"

            else ->
                null
        }

    ComicBrowseResultRow(
        badgeText = "#${result.issueNumber}",
        badgeColor = ComicBlueLight,
        title =
            "${result.seriesTitle} " +
            "#${result.issueNumber}",
        subtitle =
            result.issueTitle
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: metadata,
        supportingText =
            if (!result.issueTitle.isNullOrBlank()) {
                metadata
            } else {
                null
            },
        statusText = statusText,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ComicBrowseResultRow(
    badgeText: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    supportingText: String?,
    statusText: String?,
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
                        horizontal = 10.dp,
                        vertical = 9.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .width(52.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = RoundedCornerShape(6.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 15.sp,
                                lineHeight = 17.sp,
                                fontWeight = FontWeight.Black
                            ),
                    color = ComicInk,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 15.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = ComicInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style =
                            MaterialTheme.typography.bodySmall
                                .copy(
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp
                                ),
                        color = ComicInk.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                supportingText
                    ?.let {
                        Text(
                            text = it,
                            style =
                                MaterialTheme.typography.bodySmall
                                    .copy(
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                            color = ComicInk.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                statusText
                    ?.let { status ->
                        Box(
                            modifier =
                                Modifier
                                    .padding(
                                        top = 2.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(5.dp)
                                    )
                                    .background(
                                        when (status) {
                                            "READ" -> ComicGreen

                                            else ->
                                                ComicYellow
                                        }
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = ComicInk,
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .padding(
                                        horizontal = 7.dp,
                                        vertical = 2.dp
                                    )
                        ) {
                            Text(
                                text = status,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ComicInk
                            )
                        }
                    }
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
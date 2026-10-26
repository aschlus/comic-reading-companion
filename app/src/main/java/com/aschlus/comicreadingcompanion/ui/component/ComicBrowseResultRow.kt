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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.util.formatPublicationDate

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
        coverUrl = result.coverUrl,
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

            result.publicationDate
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    add(formatPublicationDate(it))
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
        coverUrl = result.coverUrl,
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
    title: String,
    subtitle: String,
    supportingText: String?,
    statusText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    coverUrl: String? = null,
    badgeText: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color = ComicPurple
) {
    val shape = ComicCardShape

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = ComicCardShadowOffsetX,
                        y = ComicCardShadowOffsetY
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = ComicCardBorderWidth,
                        color = MaterialTheme.colorScheme.outline,
                        shape = shape
                    )
                    .semantics {
                        contentDescription?.let {
                            this.contentDescription = it
                        }
                    }
                    .clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 9.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (!coverUrl.isNullOrBlank()) {
                ComicCoverImage(
                    coverUrl = coverUrl,
                    contentDescription = "$title cover",
                    modifier = Modifier
                        .width(52.dp)
                        .aspectRatio(2f / 3f),
                    placeholderText = "No Cover",
                    shape = RoundedCornerShape(4.dp),
                    borderColor = ComicInk
                )
            } else if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            } else {
                ComicCoverImage(
                    coverUrl = null,
                    contentDescription = "$title cover",
                    modifier = Modifier
                        .width(52.dp)
                        .aspectRatio(2f / 3f),
                    placeholderText = "No Cover",
                    shape = RoundedCornerShape(4.dp),
                    borderColor = ComicInk
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
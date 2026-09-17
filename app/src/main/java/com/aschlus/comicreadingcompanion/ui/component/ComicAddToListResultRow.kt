package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.models.IssueSearchResult
import com.aschlus.comicreadingcompanion.data.database.models.SeriesSearchResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicAddToListSeriesResult(
    result: SeriesSearchResult,
    selected: Boolean,
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
        }.joinToString(" • ")

    ComicAddToListResultRow(
        title = result.title,
        subtitle = metadata,
        supportingText = "${result.totalCount} issues",
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun ComicAddToListIssueResult(
    result: IssueSearchResult,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metadata =
        buildList {
            result.publicationDate?.let {
                add(it)
            }

            add(result.publisherName)
        }.joinToString(" • ")

    ComicAddToListResultRow(
        title = "${result.seriesTitle} #${result.issueNumber}",
        subtitle =
            result.issueTitle
                ?.takeIf { it.isNotBlank() }
                ?: metadata,
        supportingText =
            if (!result.issueTitle.isNullOrBlank()) {
                metadata
            } else {
                null
            },
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun ComicAddToListResultRow(
    title: String,
    subtitle: String,
    supportingText: String?,
    selected: Boolean,
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
                        if (selected) {
                            ComicBlueLight
                        } else {
                            ComicPaper
                        }
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
                        horizontal = 12.dp,
                        vertical = 11.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
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
                        style = MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            ),
                        color = ComicInk.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall
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
            }

            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (selected) {
                                ComicPaper
                            } else {
                                ComicPaper.copy(alpha = 0.45f)
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = RoundedCornerShape(7.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ComicInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
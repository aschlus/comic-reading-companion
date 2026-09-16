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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicReadingListDetailHero(
    description: String?,
    coverUrl: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.Top
    ) {
        ComicCoverImage(
            coverUrl = coverUrl,
            contentDescription =
                "Reading list cover",
            modifier =
                Modifier
                    .width(86.dp)
                    .aspectRatio(2f / 3f)
                    .clip(
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape =
                            RoundedCornerShape(6.dp)
                    ),
            placeholderText =
                "No Cover"
        )

        Spacer(
            modifier =
                Modifier.width(14.dp)
        )

        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                modifier =
                    Modifier.weight(1f),
                style =
                    MaterialTheme.typography
                        .bodyMedium
                        .copy(
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        ),
                color = ComicInk,
                maxLines = 6,
                overflow =
                    TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ComicReadingListDetailControls(
    readCount: Int,
    totalCount: Int,
    progress: Float,
    visibleIssueCount: Int,
    showFilteredCount: Boolean,
    activeFilterCount: Int,
    showJumpToCurrent: Boolean,
    onFiltersClick: () -> Unit,
    onJumpToCurrentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionPercentage =
        if (totalCount == 0) {
            0
        } else {
            (readCount * 100) / totalCount
        }

    Column(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Text(
            text =
                "$readCount of $totalCount read • " +
                        "$completionPercentage% complete",
            style =
                MaterialTheme.typography
                    .labelLarge
                    .copy(
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold
                    ),
            color = ComicInk
        )

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        ComicProgressBar(
            progress = progress,
            progressColor = ComicBlue,
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicReadingListFilterButton(
                activeFilterCount =
                    activeFilterCount,
                onClick =
                    onFiltersClick
            )

            Text(
                text =
                    if (showFilteredCount) {
                        "$visibleIssueCount of " +
                                "$totalCount issues"
                    } else {
                        "$totalCount issues"
                    },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(
                            horizontal = 10.dp
                        ),
                style =
                    MaterialTheme.typography
                        .bodySmall
                        .copy(
                            fontWeight =
                                FontWeight.SemiBold
                        ),
                color = ComicInk
            )

            if (showJumpToCurrent) {
                ComicJumpToCurrentButton(
                    onClick =
                        onJumpToCurrentClick
                )
            }
        }
    }
}

@Composable
private fun ComicReadingListFilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape =
        RoundedCornerShape(9.dp)

    Row(
        modifier =
            modifier
                .height(40.dp)
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
                    horizontal = 11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector =
                Icons.Default.FilterList,
            contentDescription =
                null,
            tint =
                ComicInk
        )

        Text(
            text =
                if (activeFilterCount == 0) {
                    "Filters"
                } else {
                    "Filters ($activeFilterCount)"
                },
            style =
                MaterialTheme.typography
                    .labelLarge,
            color =
                ComicInk
        )
    }
}

@Composable
private fun ComicJumpToCurrentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape =
        RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .height(40.dp)
                .clip(shape)
                .background(ComicBlue)
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 12.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text = "Jump to Current",
            style =
                MaterialTheme.typography
                    .labelLarge,
            color =
                ComicInk,
            maxLines =
                1
        )
    }
}
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.models.RecentReadIssue
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicReadingHistoryRow(
    issue: RecentReadIssue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                    .clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                    .padding(ComicSpacingSm),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicCoverImage(
                coverUrl = issue.coverUrl,
                contentDescription = "${issue.seriesTitle} #${issue.issueNumber} cover",
                modifier =
                    Modifier
                        .width(66.dp)
                        .aspectRatio(2f / 3f),
                shape = RoundedCornerShape(6.dp),
                borderColor = ComicInk
            )

            Spacer(modifier = Modifier.width(ComicSpacingMd))

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "${issue.seriesTitle} #${issue.issueNumber}",
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 17.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                issue.issueTitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { title ->
                        Text(
                            text = "“$title”",
                            style =
                                MaterialTheme.typography.bodyMedium
                                    .copy(
                                        fontSize = 14.sp,
                                        lineHeight = 17.sp
                                    ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                Text(
                    text = recentReadFinishedText(issue.completedAt),
                    style =
                        MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(ComicSpacingSm))

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
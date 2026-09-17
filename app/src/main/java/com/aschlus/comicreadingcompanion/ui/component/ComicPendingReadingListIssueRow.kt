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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.viewmodel.PendingReadingListIssue

@Composable
fun ComicPendingReadingListIssueRow(
    issue: PendingReadingListIssue,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
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
                    .background(
                        color = ComicPaper,
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .padding(
                        start = 12.dp,
                        top = 10.dp,
                        end = 8.dp,
                        bottom = 10.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            )
            {
                Text(
                    text = "${issue.seriesTitle} #${issue.issueNumber}",
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

                issue.issueTitle
                    ?.takeIf { it.isNotBlank() }
                    ?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = ComicInk.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                issue.publicationDate
                    ?.let { date ->
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall
                                .copy(
                                    fontSize = 11.sp
                                ),
                            color = ComicInk.copy(alpha = 0.6f)
                        )
                    }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                ComicPendingIssueActionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move issue up",
                    enabled = canMoveUp,
                    onClick = onMoveUp
                )

                ComicPendingIssueActionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move issue down",
                    enabled = canMoveDown,
                    onClick = onMoveDown
                )

                ComicPendingIssueActionButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Remove issue",
                    enabled = true,
                    onClick = onRemove
                )
            }
        }
    }
}

@Composable
private fun ComicPendingIssueActionButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)

    val alpha =
        if (enabled) {
            1f
        } else {
            0.28f
        }

    Box(
        modifier =
            modifier.size(34.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .offset(
                        x = 2.dp,
                        y = 3.dp
                    )
                    .background(
                        color = ComicInk.copy(alpha = alpha),
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(shape)
                    .background(
                        color = ComicPaper.copy(alpha = alpha)
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk.copy(alpha = alpha),
                        shape = shape
                    )
                    .clickable(
                        enabled = enabled,
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = ComicInk.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
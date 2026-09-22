package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingStatus
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicIssueStatusSelector(
    readingStatus: ReadingStatus?,
    onUnreadClick: () -> Unit,
    onReadingClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        ComicIssueStatusButton(
            label = "Unread",
            selected = readingStatus == null,
            onClick = onUnreadClick,
            modifier = Modifier.weight(1f)
        )

        ComicIssueStatusButton(
            label = "Reading",
            selected = readingStatus == ReadingStatus.READING,
            onClick = onReadingClick,
            modifier = Modifier.weight(1f)
        )

        ComicIssueStatusButton(
            label = "Read",
            selected = readingStatus == ReadingStatus.READ,
            onClick = onReadClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ComicIssueStatusButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .height(54.dp)
                .semantics(
                    mergeDescendants = true
                ) {
                    contentDescription =
                        if (selected) {
                            "$label reading status selected"
                        } else {
                            "$label reading status"
                        }

                    role = Role.Button
                }
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset(
                        x = 2.dp,
                        y = 3.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
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
                        enabled = !selected,
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                style =
                    MaterialTheme.typography.labelLarge
                        .copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                color = ComicInk
            )
        }
    }
}
package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem

@Composable
fun ComicRecentCard(
    readingList: ReadingList,
    readCount: Int,
    totalCount: Int,
    continueItem: ReadingListContinueItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress =
        if (totalCount == 0) {
            0f
        } else {
            readCount.toFloat() / totalCount.toFloat()
        }

    val shape = RoundedCornerShape(8.dp)

    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier =
            modifier.width(210.dp)
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset(
                        x = 4.dp,
                        y = 4.dp
                    ),
            colors =
                CardDefaults.cardColors(
                    containerColor = outlineColor
                ),
            shape = shape
        ) {}

        Card(
            onClick = onClick,
            modifier =
                Modifier.fillMaxWidth(),
            shape = shape,
            border =
                BorderStroke(
                    2.dp,
                    outlineColor
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
        ) {
            Column(
                modifier =
                    Modifier.padding(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = readingList.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val statusText =
                    when {
                        totalCount > 0 &&
                            readCount == totalCount ->
                            "Completed"

                        continueItem != null ->
                            "Next: " +
                            "${continueItem.seriesTitle} " +
                            "#${continueItem.issueNumber}"

                        else ->
                            "Open reading list"
                    }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                ComicProgressBar(
                    progress = progress,
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        }
    }
}
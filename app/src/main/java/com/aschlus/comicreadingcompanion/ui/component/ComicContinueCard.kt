package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.data.database.models.ReadingListContinueItem
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicContinueCard(
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

    val completionPercentage =
        if (totalCount == 0) {
            0
        } else {
            (readCount * 100) / totalCount
        }

    val shape =
        RoundedCornerShape(10.dp)

    val outlineColor =
        MaterialTheme.colorScheme.outline

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset(
                        x = 5.dp,
                        y = 5.dp
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
                    width = 2.dp,
                    color = outlineColor
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
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = readingList.title,
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )

                readingList.description
                    ?.let { description ->
                        Text(
                            text = description,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            maxLines = 3,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }

                Text(
                    text =
                        "$readCount of $totalCount read • " +
                                "$completionPercentage% complete",
                    style =
                        MaterialTheme
                            .typography
                            .labelMedium
                )

                ComicProgressBar(
                    progress = progress,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (continueItem != null) {
                    Button(
                        onClick = onClick,
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                contentColor =
                                    ComicInk
                            ),
                        shape =
                            RoundedCornerShape(4.dp),
                        border =
                            BorderStroke(
                                2.dp,
                                outlineColor
                            ),
                        contentPadding =
                            PaddingValues(
                                vertical = 10.dp
                            )
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(
                            text = "CONTINUE",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge
                        )
                    }

                    Text(
                        text =
                            "Next up: " +
                                    "${continueItem.seriesTitle} " +
                                    "#${continueItem.issueNumber}",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }
}

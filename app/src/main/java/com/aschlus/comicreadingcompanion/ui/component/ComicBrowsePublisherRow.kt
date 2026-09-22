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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicBrowsePublisherRow(
    publisher: PublisherBrowseResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape= RoundedCornerShape(10.dp)

    val initials =
        publisher.name
            .split(" ")
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .joinToString("") {
                it.first()
                    .uppercaseChar()
                    .toString()
            }

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 2.dp,
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
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ComicBlueLight)
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape =
                                RoundedCornerShape(8.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = initials,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 14.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Black
                            ),
                    color = ComicInk,
                    maxLines = 1
                )
            }

            Spacer(
                modifier =
                    Modifier.size(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = publisher.name,
                    style =
                        MaterialTheme.typography.titleMedium
                            .copy(
                                fontSize = 16.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold
                            ),
                    color = ComicInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text =
                        "${publisher.seriesCount} series",
                    style =
                        MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp
                            ),
                    color = ComicInk.copy(alpha = 0.7f)
                )
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
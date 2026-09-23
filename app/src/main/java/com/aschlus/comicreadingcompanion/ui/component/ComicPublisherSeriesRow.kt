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
import com.aschlus.comicreadingcompanion.data.database.models.PublisherSeries
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicOrange
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaperDeep
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRedLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

@Composable
fun ComicPublisherSeriesRow(
    series: PublisherSeries,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    val metadata =
        buildList {
            series.volume
                ?.let { volume->
                    add("Volume $volume")
                }

            when {
                series.startYear != null &&
                    series.endYear != null &&
                    series.startYear !=
                    series.endYear -> {

                        add(
                            "${series.startYear}-${series.endYear}"
                        )
                    }

                series.startYear != null -> {
                    add(series.startYear.toString())
                }
            }
        }
            .joinToString(" • ")

    val progress =
        if (series.totalCount == 0) {
            0f
        } else {
            series.readCount.toFloat() / series.totalCount.toFloat()
        }

    val badgeColors =
        listOf(
            ComicBlueLight,
            ComicPurple,
            ComicGreen,
            ComicOrange,
            ComicRedLight
        )

    val badgeColor =
        badgeColors[
            (series.seriesId % badgeColors.size).toInt()
        ]

    val badgeText =
        series.title
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"

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
                        vertical = 10.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .border(
                            width = 2.dp,
                            color = ComicInk,
                            shape = RoundedCornerShape(8.dp)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style =
                        MaterialTheme.typography.headlineMedium
                            .copy(
                                fontSize = 28.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Black
                            ),
                    color = ComicInk
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(
                modifier =
                    Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = series.title,
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

                if (metadata.isNotBlank()) {
                    Text(
                        text = metadata,
                        style =
                            MaterialTheme.typography.bodySmall
                                .copy(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp
                                ),
                        color = ComicInk.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text =
                        "${series.readCount} of ${series.totalCount} read",
                    style =
                        MaterialTheme.typography.bodySmall
                            .copy(
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                    color = ComicInk
                )

                ComicProgressBar(
                    progress = progress,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                    progressColor = ComicYellow,
                    trackColor = ComicPaperDeep,
                    borderColor = ComicInk,
                    height = 11.dp,
                    shape = RoundedCornerShape(percent = 50)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

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
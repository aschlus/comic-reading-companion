package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

@Composable
fun ComicHomeRecentReadCard(
    seriesTitle: String,
    issueNumber: String,
    coverUrl: String?,
    completedAt: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
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

        Column(
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
                    .padding(6.dp)
        ) {
            RecentReadCover(
                seriesTitle = seriesTitle,
                coverUrl = coverUrl,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
            )

            Text(
                text = seriesTitle,
                modifier =
                    Modifier.padding(
                        top = 3.dp
                    ),
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Issue #$issueNumber",
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = ComicInk
            )

            Text(
                text =
                    recentReadText(completedAt),
                modifier =
                    Modifier.padding(
                        bottom = 2.dp
                    ),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = ComicInk.copy(
                    alpha = 0.72f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentReadCover(
    seriesTitle: String,
    coverUrl: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(5.dp)

    if (coverUrl.isNullOrBlank()) {
        RecentReadPlaceholder(
            seriesTitle = seriesTitle,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model =
                ImageRequest.Builder(
                    LocalPlatformContext.current
                )
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
            contentDescription = "$seriesTitle cover",
            contentScale = ContentScale.Crop,
            modifier =
                modifier
                    .clip(shape)
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    ),
            loading = {
                RecentReadPlaceholder(
                    seriesTitle = seriesTitle,
                    modifier = Modifier.fillMaxSize()
                )
            },
            error = {
                RecentReadPlaceholder(
                    seriesTitle = seriesTitle,
                    modifier = Modifier.fillMaxSize()
                )
            },
            success = {
                SubcomposeAsyncImageContent()
            }
        )
    }
}

@Composable
private fun RecentReadPlaceholder(
    seriesTitle: String,
    modifier: Modifier = Modifier
) {
    val palette =
        listOf(
            ComicRed,
            ComicYellow,
            ComicBlue,
            ComicGreen,
            ComicPurple
        )

    val backgroundColor =
        palette[
            seriesTitle
                .hashCode()
                .and(Int.MAX_VALUE) %
                palette.size
        ]

    val initials =
        seriesTitle
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
            modifier
                .clip(
                    RoundedCornerShape(5.dp)
                )
                .background(
                    backgroundColor
                )
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = RoundedCornerShape(5.dp)
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Canvas(
            modifier =
                Modifier.fillMaxSize()
        ) {
            val center =
                Offset(
                    x = size.width / 2f,
                    y = size.height / 2f
                )

            val rayLength = size.maxDimension * 0.7f

            repeat(12) { index ->
                rotate(
                    degrees = index * 30f,
                    pivot = center
                ) {
                    drawLine(
                        color = ComicInk.copy(
                            alpha = 0.12f
                        ),
                        start = center,
                        end =
                            Offset(
                                x = center.x,
                                y = center.y - rayLength
                            ),
                        strokeWidth = 7.dp.toPx()
                    )
                }
            }
        }

        Text(
            text = initials,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = ComicPaper
        )
    }
}
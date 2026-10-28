package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicDataRowShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicDataRowShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd

@Composable
fun ComicBrowsePublisherRow(
    publisher: PublisherBrowseResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = ComicCardShape

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

    val coverUrls =
        listOfNotNull(
            publisher.coverUrl1,
            publisher.coverUrl2,
            publisher.coverUrl3
        )
            .filter { it.isNotBlank() }

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = ComicDataRowShadowOffsetX,
                        y = ComicDataRowShadowOffsetY
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
                    .padding(
                        horizontal = ComicSpacingMd,
                        vertical = 10.dp
                    )
                    .semantics {
                        contentDescription =
                            "Open ${publisher.name}, ${publisher.seriesCount} series"
                    },
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            PublisherCoverCollage(
                coverUrls = coverUrls,
                publisherId = publisher.publisherId,
                initials = initials
            )

            Spacer(
                modifier =
                    Modifier.size(ComicSpacingMd)
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
                    color = MaterialTheme.colorScheme.onSurface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

@Composable
private fun PublisherCoverCollage(
    coverUrls: List<String>,
    publisherId: Long,
    initials: String,
    modifier: Modifier = Modifier
) {
    val fallbackShape = RoundedCornerShape(10.dp)
    val coverShape = RoundedCornerShape(4.dp)
    val covers = coverUrls.take(3)
    val collageWidth = if (covers.isEmpty()) 64.dp else 112.dp
    val collageHeight = if (covers.isEmpty()) 64.dp else 78.dp

    Box(
        modifier =
            modifier
                .size(
                    width = collageWidth,
                    height = collageHeight
                ),
        contentAlignment = Alignment.TopStart
    ) {
        if (covers.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(fallbackShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = fallbackShape
                        ),
                contentAlignment = Alignment.Center
            ) {
                ComicArtworkBackground(
                    seed = publisherId,
                    accentColor = ComicBlueLight,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium
                        .copy(
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Black
                        ),
                    color = ComicInk,
                    maxLines = 1
                )
            }
        } else {
            covers
                .asReversed()
                .forEachIndexed { index, coverUrl ->
                    val placement = publisherCoverPlacement(
                        coverCount = covers.size,
                        index = index
                    )

                    ComicCoverImage(
                        coverUrl = coverUrl,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .offset(
                                    x = placement.x,
                                    y = placement.y
                                )
                                .graphicsLayer {
                                    rotationZ = placement.rotation
                                }
                                .size(
                                    width = 48.dp,
                                    height = 70.dp
                                ),
                        placeholderText = "",
                        shape = coverShape,
                        borderColor = ComicInk,
                        contentScale = ContentScale.Fit
                    )
                }
        }
    }
}

private data class PublisherCoverPlacement(
    val x: Dp,
    val y: Dp,
    val rotation: Float
)

private fun publisherCoverPlacement(
    coverCount: Int,
    index: Int
): PublisherCoverPlacement =
    when (coverCount) {
        1 -> PublisherCoverPlacement(32.dp, 4.dp, 0f)
        2 -> listOf(
            PublisherCoverPlacement(14.dp, 4.dp, -6f),
            PublisherCoverPlacement(50.dp, 4.dp, 6f)
        )[index]
        else -> listOf(
            PublisherCoverPlacement(3.dp, 6.dp, -8f),
            PublisherCoverPlacement(61.dp, 6.dp, 8f),
            PublisherCoverPlacement(32.dp, 4.dp, 0f)
        )[index]
    }

package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicGray
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicOrange
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ComicArtworkPattern {
    BURST,
    SPEED_LINES,
    HALFTONE,
    PANELS
}

fun readingListAccentColor(
    style: ReadingListStyle
): Color =
    when (style) {
        ReadingListStyle.GREEN ->
            ComicGreen

        ReadingListStyle.RED ->
            ComicRed

        ReadingListStyle.BLUE ->
            ComicBlue

        ReadingListStyle.PURPLE ->
            ComicPurple

        ReadingListStyle.ORANGE ->
            ComicOrange

        ReadingListStyle.GRAY ->
            ComicGray
    }

@Composable
fun ComicArtworkBackground(
    seed: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val patterns = ComicArtworkPattern.entries

    val patternIndex =
        ((seed % patterns.size) + patterns.size)
            .toInt() % patterns.size

    val pattern = patterns[patternIndex]

    Canvas(
        modifier =
            modifier
                .background(accentColor)
    ) {
        val ink = ComicInk.copy(alpha = 0.18f)

        when (pattern) {
            ComicArtworkPattern.BURST -> {
                val center =
                    Offset(
                        x = size.width * (0.42f +
                            ((seed % 9L).toFloat() * 0.01f)),
                        y = size.height * 0.55f
                    )

                val rayLength = size.maxDimension * 0.8f

                val phase =
                    ((seed % 17L).toFloat() / 17f) *
                        (PI * 2.0)

                repeat(18) { index ->
                    val angle =
                        phase + ((PI * 2.0) * index / 18.0)

                    drawLine(
                        color = ink,
                        start = center,
                        end =
                            Offset(
                                x = center.x + cos(angle)
                                    .toFloat() * rayLength,
                                y = center.y + sin(angle)
                                    .toFloat() * rayLength
                            ),
                        strokeWidth =
                            if (index % 2 == 0) {
                                7.dp.toPx()
                            } else {
                                3.dp.toPx()
                            }
                    )
                }
            }

            ComicArtworkPattern.SPEED_LINES -> {
                repeat(12) { index ->
                    val y = size.height * ((index + 1f) / 13f)

                    val lengthOffset = ((seed + index * 13L) % 7L).toFloat() /7f

                    drawLine(
                        color = ink,
                        start =
                            Offset(
                                x = -size.width * 0.12f,
                                y = y
                            ),
                        end =
                            Offset(
                                x = size.width * (0.65f + lengthOffset * 0.45f),
                                y = y - size.height * 0.28f
                            ),
                        strokeWidth =
                            if (index % 3 == 0) {
                                6.dp.toPx()
                            } else {
                                3.dp.toPx()
                            }
                    )
                }
            }

            ComicArtworkPattern.HALFTONE -> {
                val spacing = 11.dp.toPx()

                var y = spacing / 2f

                while (y < size.height) {
                    var x = spacing / 2f

                    while (x < size.width) {
                        val distanceFactor = ((x / size.width) + (y / size.height)) / 2f

                        val radius = 1.1.dp.toPx() + (2.2.dp.toPx() * distanceFactor)

                        drawCircle(
                            color = ink,
                            radius = radius,
                            center =
                                Offset(
                                    x = x,
                                    y = y
                                )
                        )

                        x += spacing
                    }

                    y += spacing
                }
            }

            ComicArtworkPattern.PANELS -> {
                drawLine(
                    color = ink,
                    start =
                        Offset(
                            x = size.width * 0.18f,
                            y = size.height
                        ),
                    end =
                        Offset(
                            x = size.width * 0.48f,
                            y = size.height
                        ),
                    strokeWidth = 9.dp.toPx()
                )

                drawLine(
                    color = ink,
                    start =
                        Offset(
                            x = size.width,
                            y = size.height * 0.18f
                        ),
                    end =
                        Offset(
                            x = 0f,
                            y = size.height * 0.76f
                        ),
                    strokeWidth = 7.dp.toPx()
                )

                drawLine(
                    color = ComicPaper.copy(alpha = 0.35f),
                    start =
                        Offset(
                            x = size.width * 0.72f,
                            y = 0f
                        ),
                    end =
                        Offset(
                            x = size.width * 0.92f,
                            y = size.height
                        ),
                    strokeWidth = 5.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun ComicReadingListArtwork(
    title: String,
    coverUrls: List<String?>,
    seed: Long,
    accentColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    val usableCovers =
        coverUrls
            .filterNotNull()
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)

    val initials =
        title
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") {
                it.first()
                    .uppercaseChar()
                    .toString()
            }

    Box(
        modifier =
            modifier
                .clip(shape)
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .semantics {
                    contentDescription = "$title artwork"
                }
    ) {
        when (usableCovers.size) {
            0 -> {
                ComicArtworkBackground(
                    seed = seed,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = initials,
                    modifier =
                        Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.headlineMedium
                        .copy(
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            drawStyle =
                                Stroke(
                                    width = 5f
                                )
                        ),
                    color = ComicInk
                )

                Text(
                    text = initials,
                    modifier =
                        Modifier.align(Alignment.Center),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    color = ComicPaper
                )
            }

            1 -> {
                ArtworkCover(
                    coverUrl = usableCovers[0],
                    modifier = Modifier.fillMaxSize()
                )
            }

            2 -> {
                Row(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    usableCovers.forEach { cover ->
                        ArtworkCover(
                            coverUrl = cover,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                        )
                    }
                }
            }

            3 -> {
                Row(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    ArtworkCover(
                        coverUrl = usableCovers[0],
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                    ) {
                        ArtworkCover(
                            coverUrl = usableCovers[1],
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                        )

                        ArtworkCover(
                            coverUrl = usableCovers[2],
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                        )
                    }
                }
            }

            else -> {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    repeat(2) { row ->
                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                        ) {
                            repeat(2) { column ->
                                val index = row * 2 + column

                                ArtworkCover(
                                    coverUrl = usableCovers[index],
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkCover(
    coverUrl: String,
    modifier: Modifier = Modifier
) {
    ComicCoverImage(
        coverUrl = coverUrl,
        contentDescription = null,
        modifier = modifier,
        placeholderText = "",
        shape = RectangleShape,
        borderColor = ComicInk
    )
}
package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import kotlin.math.max

private val HomeHeaderContentHeight = 136.dp

@Composable
fun ComicHomeHeader(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(ComicBlue)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()

                    drawLine(
                        color = ComicInk,
                        start =
                            Offset(
                                x = 0f,
                                y = size.height - strokeWidth / 2f
                            ),
                        end =
                            Offset(
                                x = size.width,
                                y = size.height - strokeWidth / 2f
                            ),
                        strokeWidth = strokeWidth
                    )
                }
    ) {
        ComicHalftoneOverlay(
            modifier = Modifier.matchParentSize(),
            dotColor = ComicInk,
            spacing = 10.dp,
            radius = 1.1.dp,
            startFraction = 0f,
            minAlpha = 0.3f,
            maxAlpha = 0.14f
        )

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Spacer(
                modifier =
                    Modifier.windowInsetsTopHeight(
                        WindowInsets.statusBars
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            HomeHeaderContentHeight
                        )
            ) {
                HomeTitle(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .padding(
                                start = 25.dp
                            )
                )

                SettingsButton(
                    onClick = onSettingsClick,
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .padding(
                                end = 18.dp
                            )
                )
            }
        }
    }
}

@Composable
private fun HomeTitle(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val outlineWidth =
        with (density) {
            1.8.dp.toPx()
        }

    val titleStyle =
        MaterialTheme.typography.displayLarge
            .copy(
                fontSize = 54.sp,
                lineHeight = 54.sp,
                letterSpacing = (-2).sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )

    Box(
        modifier = modifier
    ) {
        DecorativeHomeTitle(
            modifier =
                Modifier.offset(
                    x = 5.dp,
                    y = 7.dp
                ),
            style = titleStyle,
            color = ComicInk
        )

        DecorativeHomeTitle(
            style =
                titleStyle.copy(
                    drawStyle =
                        Stroke(
                            width = outlineWidth
                        )
                ),
            color = ComicInk
        )

        Text(
            text = "HOME",
            style = titleStyle,
            color = ComicYellow
        )
    }
}

@Composable
private fun DecorativeHomeTitle(
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = "HOME",
        modifier = modifier.clearAndSetSemantics { },
        style = style,
        color = color
    )
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier
                .size(58.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(shape)
                    .background(
                        color = ComicYellow
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    )
                    .semantics {
                        contentDescription = "Settings"
                    },
            contentAlignment =
                Alignment.Center
        ) {
            ComicHalftoneOverlay(
                modifier =
                    Modifier.fillMaxSize(),
                dotColor = ComicInk,
                spacing = 8.dp,
                radius = 0.7.dp,
                startFraction = 0.55f,
                minAlpha = 0.08f,
                maxAlpha = 0.16f
            )

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = ComicInk,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
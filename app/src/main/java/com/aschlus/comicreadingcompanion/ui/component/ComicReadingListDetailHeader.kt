package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

internal val ReadingListDetailHeaderContentHeight = 86.dp

@Composable
fun ComicReadingListDetailHeader(
    title: String,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    menuContent: @Composable () -> Unit,
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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            ReadingListDetailHeaderContentHeight
                        )
                        .padding(
                            horizontal = 16.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                ComicReadingListDetailHeaderButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBackClick
                )

                AdaptiveReadingListHeaderTitle(
                    title = title,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 14.dp
                            )
                )
                ComicReadingListDetailHeaderButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search reading list",
                    onClick = onSearchClick
                )

                Spacer(modifier = Modifier.size(8.dp))

                Box {
                    ComicReadingListDetailHeaderButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Reading list options",
                        onClick = onMenuClick
                    )

                    menuContent()
                }
            }
        }
    }
}

@Composable
internal fun ComicReadingListDetailHeaderButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.size(56.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(50.dp)
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
                    .size(50.dp)
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
                        this.contentDescription = contentDescription
                    },
            contentAlignment =
                Alignment.Center
        ) {
            ComicHalftoneOverlay(
                modifier =
                    Modifier.matchParentSize(),
                dotColor = ComicInk,
                spacing = 8.dp,
                radius = 0.7.dp,
                startFraction = 0.55f,
                minAlpha = 0.08f,
                maxAlpha = 0.16f
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ComicInk,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AdaptiveReadingListHeaderTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    var useThreeLines by
            remember(title) {
                mutableStateOf(false)
            }
    var useTwoLines by
    remember(title) {
        mutableStateOf(false)
    }

    val titleStyle =
        MaterialTheme.typography.headlineLarge
            .copy(
                lineHeight = 1.05.em,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic
            )

    if (useThreeLines) {
        Text(
            text = title,
            modifier = modifier,
            style = titleStyle.copy(
                fontSize = 16.sp
            ),
            color = ComicInk,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    } else if (useTwoLines) {
        Text(
            text = title,
            modifier = modifier,
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = 16.sp,
                    maxFontSize = 25.sp,
                    stepSize = 0.5.sp
                ),
            style = titleStyle,
            color = ComicInk,
            maxLines = 2,
            overflow =
                TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow) {
                    useThreeLines = true
                }
            }
        )
    } else {
        Text(
            text = title,
            modifier = modifier,
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = 23.sp,
                    maxFontSize = 28.sp,
                    stepSize = 0.5.sp
                ),
            style = titleStyle,
            color = ComicInk,
            maxLines = 1,
            overflow =
                TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (result.hasVisualOverflow) {
                    useTwoLines = true
                }
            }
        )
    }
}
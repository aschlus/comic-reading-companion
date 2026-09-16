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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicReadingListSelectionHeader(
    selectedCount: Int,
    onCancelClick: () -> Unit,
    onMarkReadClick: () -> Unit,
    onMarkUnreadClick: () -> Unit,
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
                    icon = Icons.Default.Close,
                    contentDescription = "Cancel selection",
                    onClick = onCancelClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$selectedCount selected",
                    modifier = Modifier.weight(1f),
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = 16.sp,
                            maxFontSize = 22.sp,
                            stepSize = 0.5.sp
                        ),
                    style =
                        MaterialTheme.typography.headlineSmall
                            .copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic
                            ),
                    color = ComicInk,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                ComicSelectionActionButton(
                    text = "READ",
                    contentDescription = "Mark selected issues as read",
                    onClick = onMarkReadClick
                )

                Spacer(modifier = Modifier.width(6.dp))

                ComicSelectionActionButton(
                    text = "UNREAD",
                    contentDescription = "Mark selected issues as unread",
                    onClick = onMarkUnreadClick
                )
            }
        }
    }
}

@Composable
private fun ComicSelectionActionButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ComicPaper
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .width(70.dp)
                .height(54.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .width(66.dp)
                    .height(48.dp)
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
                    .width(66.dp)
                    .height(48.dp)
                    .clip(shape)
                    .background(
                        backgroundColor
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                style =
                    MaterialTheme.typography.labelMedium
                        .copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                color = ComicInk,
                maxLines = 1
            )
        }
    }
}
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicReadingListSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onCloseClick: () -> Unit,
    focusRequester: FocusRequester,
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
                    contentDescription = "Close search",
                    onClick = onCloseClick
                )

                Spacer(modifier = Modifier.size(12.dp))

                ComicReadingListSearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ComicReadingListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.height(50.dp)
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
                    .matchParentSize()
                    .clip(shape)
                    .background(
                        ComicPaper
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .padding(
                        start = 12.dp,
                        end = 6.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ComicInk,
                modifier = Modifier.size(24.dp)
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge
                        .copy(
                            color = ComicInk,
                            fontWeight = FontWeight.Medium
                        ),
                cursorBrush = SolidColor(ComicInk),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment =
                            Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search reading list",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ComicInk.copy(alpha = 0.55f)
                            )
                        }

                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onQueryChange("")
                            },
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = ComicInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
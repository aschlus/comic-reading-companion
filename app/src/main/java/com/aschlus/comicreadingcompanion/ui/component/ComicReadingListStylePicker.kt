package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingListStyle
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicGray
import com.aschlus.comicreadingcompanion.ui.theme.ComicGreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicOrange
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicPurple
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

@Composable
fun ComicReadingListStylePicker(
    selectedStyle: ReadingListStyle,
    onStyleSeclected: (ReadingListStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        ReadingListStyle.entries.forEach { style ->
            ComicReadingListStyleOption(
                style = style,
                selected = style == selectedStyle,
                onClick = {
                    onStyleSeclected(style)
                }
            )
        }
    }
}

@Composable
private fun ComicReadingListStyleOption(
    style: ReadingListStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    val backgroundColor =
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

    Box(
        modifier =
            modifier.size(50.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(46.dp)
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
                    .size(46.dp)
                    .clip(shape)
                    .background(
                        color = backgroundColor
                    )
                    .border(
                        width =
                            if (selected) {
                                3.dp
                            } else {
                                2.dp
                            },
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            ComicHalftoneOverlay(
                modifier = Modifier.fillMaxSize(),
                dotColor = ComicInk,
                spacing = 7.dp,
                radius = 0.65.dp,
                startFraction = 0.35f,
                minAlpha = 0.04f,
                maxAlpha = 0.13f
            )

            if (selected) {
                Box(
                    modifier =
                        Modifier
                            .size(25.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(ComicPaper)
                            .border(
                                width = 2.dp,
                                color = ComicInk,
                                shape = RoundedCornerShape(7.dp)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = ComicInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
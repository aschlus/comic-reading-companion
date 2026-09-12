package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicSectionBanner(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.secondary
) {
    Box(
        modifier =
            modifier
                .wrapContentWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(
                        start = 5.dp,
                        top = 5.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .background(
                        color = backgroundColor,
                        shape = ComicSlantedShape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 8.dp
                    )
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = ComicInk
            )
        }
    }
}
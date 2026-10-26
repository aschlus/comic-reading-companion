package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetX
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShadowOffsetY
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardShape
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk

@Composable
fun ComicEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    val shape = ComicCardShape

    Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = ComicCardShadowOffsetX,
                        y = ComicCardShadowOffsetY
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = shape
                    )
                    .border(
                        width = ComicCardBorderWidth,
                        color = MaterialTheme.colorScheme.outline,
                        shape = shape
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 18.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = message,
                style =
                    MaterialTheme.typography.bodyMedium
                        .copy(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
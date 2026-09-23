package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.fillMaxWidth()
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = ComicPaper,
                        shape = shape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
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
                color = ComicMutedInk,
                textAlign = TextAlign.Center
            )
        }
    }
}
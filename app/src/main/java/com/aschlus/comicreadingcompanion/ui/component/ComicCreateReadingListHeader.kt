package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

@Composable
fun ComicCreateReadingListHeader(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ComicPrimaryHeader(
        modifier = modifier
    ) {
        Row(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start = 18.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicPrimaryHeaderBackButton(
                onClick = onBackClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            ComicPrimaryHeaderTitle(
                text = "NEW LIST"
            )
        }
    }
}

@Composable
internal fun ComicPrimaryHeaderBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier =
            modifier.size(58.dp)
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
                        contentDescription = "Back"
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = ComicInk,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
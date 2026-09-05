package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun ComicCoverImage(
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholderText: String = "No Cover"
) {
    val shape = RoundedCornerShape(12.dp)

    if (coverUrl.isNullOrBlank()) {
        CoverPlaceholder(
            modifier = modifier,
            placeholderText = placeholderText
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(
                LocalPlatformContext.current
            )
                .data(coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(shape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape
                ),
            loading = {
                CoverPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    placeholderText = "Loading..."
                )
            },
            error = {
                CoverPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    placeholderText = placeholderText
                )
            },
            success = {
                SubcomposeAsyncImageContent()
            }
        )
    }
}

@Composable
private fun CoverPlaceholder(
    modifier: Modifier = Modifier,
    placeholderText: String
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = placeholderText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}
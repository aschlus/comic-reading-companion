package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

@Composable
fun ComicBrowseHeader(
    modifier: Modifier = Modifier
) {
    ComicPrimaryHeader(
        modifier = modifier
    ) {
        ComicPrimaryHeaderTitle(
            text = "BROWSE",
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start = 25.dp
                    )
        )
    }
}
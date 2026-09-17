package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ComicAddToListHeader(
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
                text = "ADD TO LIST"
            )
        }
    }
}
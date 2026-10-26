package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

@Composable
fun ComicAddToListActionBar(
    selectedCount: Int,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface
                )
                .border(
                    width = ComicCardBorderWidth,
                    color = MaterialTheme.colorScheme.outline
                )
                .navigationBarsPadding()
                .padding(
                    horizontal = ComicSpacingLg,
                    vertical = ComicSpacingMd
                )
    ) {
        Text(
            text =
                if (selectedCount == 1) {
                    "1 issue selected"
                } else {
                    "$selectedCount issues selected"
                },
            style = MaterialTheme.typography.labelLarge
                .copy(
                    fontWeight = FontWeight.ExtraBold
                ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = ComicSpacingSm
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicWideActionButton(
                text = "BACK",
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = onBackClick,
                modifier = Modifier.weight(1f)
            )

            ComicWideActionButton(
                text = "DONE",
                backgroundColor = ComicYellow,
                onClick = onDoneClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
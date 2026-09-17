package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
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
                    ComicPaper
                )
                .border(
                    width = 2.dp,
                    color = ComicInk
                )
                .navigationBarsPadding()
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
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
            color = ComicInk
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            ComicWideActionButton(
                text = "BACK",
                backgroundColor = ComicPaper,
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
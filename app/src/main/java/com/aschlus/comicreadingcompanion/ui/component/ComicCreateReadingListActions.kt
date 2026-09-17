package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow

@Composable
fun ComicCreateReadingListActions(
    canSave: Boolean,
    onCancelClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        ComicWideActionButton(
            text = "CANCEL",
            backgroundColor = ComicPaper,
            onClick = onCancelClick,
            modifier = Modifier.weight(1f)
        )

        ComicWideActionButton(
            text = "SAVE LIST",
            backgroundColor = ComicYellow,
            onClick = onSaveClick,
            modifier = Modifier.weight(1f),
            enabled = canSave
        )
    }
}
package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlueLight
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.theme.ComicHeaderTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

@Composable
fun ComicLibraryHeader(
    onCreateReadingListClick: () -> Unit,
    onImportReadingListClick: () -> Unit,
    isImporting: Boolean,
    modifier: Modifier = Modifier
) {
    ComicPrimaryHeader(
        modifier = modifier
    ) {
        ComicPrimaryHeaderTitle(
            text = "LIBRARY",
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start = 25.dp
                    )
        )

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end = 16.dp
                    )
                    .width(142.dp)
        ) {
            ComicLibraryActionButton(
                text = "CREATE\nLIST",
                icon = Icons.Default.Add,
                backgroundColor = ComicYellow,
                onClick = onCreateReadingListClick
            )

            ComicLibraryActionButton(
                text =
                    if (isImporting) {
                        "IMPORTING…"
                    } else {
                        "IMPORT\nLIST"
                    },
                icon = Icons.Default.Upload,
                backgroundColor = ComicBlueLight,
                onClick = onImportReadingListClick,
                modifier =
                    Modifier.padding(
                        top = 7.dp
                    ),
                enabled = !isImporting
            )
        }
    }
}
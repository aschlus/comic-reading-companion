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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed

data class ComicSectionPickerOption(
    val id: Long,
    val title: String
)

@Composable
fun ComicSectionPickerDialog(
    issueLabel: String,
    currentSectionId: Long?,
    sections: List<ComicSectionPickerOption>,
    onSectionSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        val shape = RoundedCornerShape(14.dp)

        Box(
            modifier =
                modifier.fillMaxWidth()
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .offset(
                            x = 4.dp,
                            y = 5.dp
                        )
                        .background(
                            color = ComicInk,
                            shape = shape
                        )
            )

            Column(
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
                        .padding(20.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                ComicSectionBanner(
                    text = "Move to Section",
                    backgroundColor = ComicRed
                )

                Text(
                    text = issueLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ComicInk
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 320.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    item(
                        key = "no-section"
                    ) {
                        ComicSectionPickerRow(
                            text = "No section",
                            selected = currentSectionId == null,
                            onClick = {
                                onSectionSelected(
                                    null
                                )
                            }
                        )
                    }

                    items(
                        items = sections,
                        key = { section -> section.id }
                    ) { section ->
                        ComicSectionPickerRow(
                            text = section.title,
                            selected = currentSectionId == section.id,
                            onClick = {
                                onSectionSelected(
                                    section.id
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.End
                ) {
                    ComicDialogButton(
                        text = "Cancel",
                        backgroundColor = ComicPaper,
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicSectionPickerRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(
                    color =
                        if (selected) {
                            ComicBlue
                        } else {
                            Color.White
                        },
                    shape = shape
                )
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .clickable(
                    enabled = !selected,
                    onClick = onClick
                )
                .padding(
                    horizontal = 14.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
                .copy(
                    fontWeight =
                        if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                ),
            color = ComicInk,
            maxLines = 1
        )

        if (selected) {
            Text(
                text = "CURRENT",
                style =
                    MaterialTheme.typography.labelSmall
                        .copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                color = ComicInk
            )
        }
    }
}
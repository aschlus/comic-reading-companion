package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm

@Composable
fun ComicBrowsePublisherFilter(
    publishers: List<PublisherBrowseResult>,
    selectedPublisherId: Long?,
    onPublisherSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedPublisher =
        publishers.firstOrNull { publisher ->
            publisher.publisherId == selectedPublisherId
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ComicSpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ComicBrowsePublisherFilterButton(
                text = selectedPublisher?.let { publisher ->
                    "PUBLISHER: ${publisher.name}"
                } ?: "PUBLISHER: ALL",
                onClick = {
                    expanded = true
                },
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Text("All publishers")
                    },
                    onClick = {
                        onPublisherSelected(null)
                        expanded = false
                    }
                )

                publishers.forEach { publisher ->
                    DropdownMenuItem(
                        text = {
                            Text(publisher.name)
                        },
                        onClick = {
                            onPublisherSelected(
                                publisher.publisherId
                            )
                            expanded = false
                        }
                    )
                }
            }
        }

        if (selectedPublisher != null) {
            Spacer(modifier = Modifier.weight(1f))
            ComicBrowsePublisherFilterButton(
                text = "CLEAR FILTERS",
                onClick = {
                    onPublisherSelected(null)
                }
            )
        }
    }
}

@Composable
private fun ComicBrowsePublisherFilterButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier = modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 3.dp)
                .background(
                    color = ComicInk,
                    shape = shape
                )
        )

        Box(
            modifier = Modifier
                .height(41.dp)
                .clip(shape)
                .background(ComicPaper)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = ComicInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
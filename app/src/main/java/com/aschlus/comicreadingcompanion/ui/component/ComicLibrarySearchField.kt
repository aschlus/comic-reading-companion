package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXs
import com.aschlus.comicreadingcompanion.ui.theme.ComicMutedInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicLibrarySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        modifier =
            modifier
                .height(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = shape
                )
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .semantics {
                    contentDescription = "Search reading lists"
                },
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(
                            start = 14.dp,
                            end = ComicSpacingXs
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(25.dp)
                )

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(
                                start = 10.dp
                            ),
                    contentAlignment =
                        Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Search reading lists...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    innerTextField()
                }

                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear reading list search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    )
}
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
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
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
            color = ComicInk
        ),
        cursorBrush = SolidColor(ComicInk),
        modifier =
            modifier
                .height(42.dp)
                .background(
                    color = ComicPaper,
                    shape = shape
                )
                .border(
                    width = 2.dp,
                    color = ComicInk,
                    shape = shape
                )
                .semantics {
                    contentDescription = "Serach reading lists"
                },
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(
                            start = 14.dp,
                            end = 4.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ComicInk,
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
                            color = ComicMutedInk
                        )
                    }

                    innerTextField()
                }

                if (value.isNotEmpty()) {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear reading list search",
                            tint = ComicInk
                        )
                    }
                }
            }
        }
    )
}
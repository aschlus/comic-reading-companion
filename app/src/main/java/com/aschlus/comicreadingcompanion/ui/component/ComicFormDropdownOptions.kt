package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper

@Composable
fun ComicFormDropdownOptions(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(9.dp)

    androidx.compose.foundation.layout.Box(
        modifier =
            modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
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
                    ),
            content = content
        )
    }
}

@Composable
fun ComicFormDropdownOption(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
        style = MaterialTheme.typography.labelLarge
            .copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
        color = ComicInk
    )
}
package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddToListFilter

@Composable
fun ComicAddToListFilters(
    selectedFilter: AddToListFilter,
    onFilterSelected: (AddToListFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier =
            modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {
        AddToListFilter.entries.forEach { filter ->

            ComicAddToListFilterButton(
                text =
                    when (filter) {
                        AddToListFilter.ALL ->
                            "ALL"

                        AddToListFilter.SERIES ->
                            "SERIES"

                        AddToListFilter.ISSUES ->
                            "ISSUES"
                    },
                selected = filter == selectedFilter,
                onClick = {
                    onFilterSelected(filter)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ComicAddToListFilterButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier.height(44.dp)
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .offset(
                            x = 2.dp,
                            y = 3.dp
                        )
                        .background(
                            color = ComicInk,
                            shape = shape
                        )
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(43.dp)
                    .clip(shape)
                    .background(
                        if (selected) {
                            ComicBlue
                        } else {
                            ComicPaper
                        }
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable(
                        onClick = onClick
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
                    .copy(
                        fontWeight =
                            if (selected) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.SemiBold
                            }
                    ),
                color = ComicInk
            )
        }
    }
}
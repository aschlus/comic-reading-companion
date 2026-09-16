package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.theme.ComicAccentTextTransform
import com.aschlus.comicreadingcompanion.ui.theme.ComicBlue
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.LilitaOneFontFamily

data class ComicReadingListFilterSeriesOption(
    val key: String,
    val label: String
)

@Composable
fun ComicReadingListFilterSheetContent(
    readingStatusFilter: String,
    onReadingStatusFilterChange: (String) -> Unit,
    requiredFilter: String,
    onRequiredFilterChange: (String) -> Unit,
    selectedSeriesLabel: String,
    seriesOptions: List<ComicReadingListFilterSeriesOption>,
    onSeriesSelected: (String?) -> Unit,
    activeFilterCount: Int,
    visibleIssueCount: Int,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 20.dp
                ),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth(),
            contentAlignment =
                Alignment.Center
        ) {
            Box(
                modifier =
                    Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .background(
                            color = ComicInk,
                            shape = RoundedCornerShape(2.dp)
                        )
            )
        }

        ComicFilterTitleBanner(
            text = "FILTER ISSUES"
        )

        ComicFilterSectionLabel(
            text = "READING STATUS"
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ComicFilterChoice(
                text = "Unread",
                selected = readingStatusFilter == "UNREAD",
                onClick = {
                    onReadingStatusFilterChange(
                        if (readingStatusFilter == "UNREAD") {
                            "ALL"
                        } else {
                            "UNREAD"
                        }
                    )
                }
            )

            ComicFilterChoice(
                text = "Reading",
                selected = readingStatusFilter == "READING",
                onClick = {
                    onReadingStatusFilterChange(
                        if (readingStatusFilter == "READING") {
                            "ALL"
                        } else {
                            "READING"
                        }
                    )
                }
            )

            ComicFilterChoice(
                text = "Read",
                selected = readingStatusFilter == "READ",
                onClick = {
                    onReadingStatusFilterChange(
                        if (readingStatusFilter == "READ") {
                            "ALL"
                        } else {
                            "READ"
                        }
                    )
                }
            )
        }

        ComicFilterSectionLabel(
            text = "LIST STATUS"
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ComicFilterChoice(
                text = "Required",
                selected = requiredFilter == "REQUIRED",
                onClick = {
                    onRequiredFilterChange(
                        if (requiredFilter == "REQUIRED") {
                            "ALL"
                        } else {
                            "REQUIRED"
                        }
                    )
                }
            )

            ComicFilterChoice(
                text = "Optional",
                selected = requiredFilter == "OPTIONAL",
                onClick = {
                    onRequiredFilterChange(
                        if (requiredFilter == "OPTIONAL") {
                            "ALL"
                        } else {
                            "OPTIONAL"
                        }
                    )
                }
            )
        }

        ComicFilterSectionLabel(
            text = "SERIES"
        )

        ComicSeriesFilterSelector(
            selectedSeriesLabel = selectedSeriesLabel,
            seriesOptions = seriesOptions,
            onSeriesSelected = onSeriesSelected
        )

        Spacer(modifier = Modifier.height(2.dp))

        if (activeFilterCount > 0) {
            ComicFilterSecondaryButton(
                text = "Clear all filters",
                onClick = onClearAll
            )
        }

        ComicFilterPrimaryButton(
            text =
                if (activeFilterCount > 0) {
                    "Show $visibleIssueCount issues"
                } else {
                    "Done"
                },
            onClick = onDone
        )
    }
}

@Composable
private fun ComicFilterTitleBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val outlineWidth =
        with (density) {
            7.dp.toPx()
        }

    val titleStyle =
        TextStyle(
            fontFamily = LilitaOneFontFamily,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            textGeometricTransform = ComicAccentTextTransform
        )

    Box(
        modifier =
            modifier
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
        )

        Box(
            modifier =
                Modifier
                    .background(
                        color = ComicRed,
                        shape = ComicSlantedShape
                    )
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = ComicSlantedShape
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
            contentAlignment =
                Alignment.CenterStart
        ) {
            Text(
                text = text,
                modifier =
                    Modifier
                        .offset(y = 1.dp)
                        .clearAndSetSemantics { },
                style =
                    titleStyle.copy(
                        drawStyle =
                            Stroke(
                                width = outlineWidth,
                                join = StrokeJoin.Round
                            )
                    ),
                color = ComicInk,
                maxLines = 1
            )

            Text(
                text = text,
                modifier =
                    Modifier.offset(
                        y = 1.dp
                    ),
                style = titleStyle,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ComicFilterSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge
            .copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            ),
        color = ComicInk
    )
}

@Composable
private fun ComicFilterChoice(
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
                            y = 2.dp
                        )
                        .background(
                            color = ComicInk,
                            shape = shape
                        )
            )
        }

        Box(
            modifier =
                modifier
                    .height(42.dp)
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
                    )
                    .padding(
                        horizontal = 14.dp
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

@Composable
private fun ComicSeriesFilterSelector(
    selectedSeriesLabel: String,
    seriesOptions: List<ComicReadingListFilterSeriesOption>,
    onSeriesSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(shape)
                    .background(ComicPaper)
                    .border(
                        width = 2.dp,
                        color = ComicInk,
                        shape = shape
                    )
                    .clickable {
                        menuExpanded = true
                    }
                    .padding(
                        start = 14.dp,
                        end = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = selectedSeriesLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
                    .copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                color = ComicInk,
                maxLines = 1
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Choose series",
                tint = ComicInk,
                modifier = Modifier.size(26.dp)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = {
                menuExpanded = false
            }
        ) {
            DropdownMenuItem(
                text = {
                    Text("All series")
                },
                onClick = {
                    onSeriesSelected(null)
                    menuExpanded = false
                }
            )

            seriesOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(option.label)
                    },
                    onClick = {
                        onSeriesSelected(option.key)
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ComicFilterSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .background(ComicPaper)
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
                    fontWeight = FontWeight.Bold
                ),
            color = ComicInk
        )
    }
}

@Composable
private fun ComicFilterPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .offset(
                        x = 3.dp,
                        y = 4.dp
                    )
                    .background(
                        color = ComicInk,
                        shape = shape
                    )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(shape)
                    .background(ComicBlue)
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
                        fontWeight = FontWeight.ExtraBold
                    ),
                color = ComicInk
            )
        }
    }
}
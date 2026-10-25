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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.Universe
import com.aschlus.comicreadingcompanion.data.database.models.PublisherBrowseResult
import com.aschlus.comicreadingcompanion.ui.theme.ComicCardBorderWidth
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacing2Xl
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingSm
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingXl

private data class ComicBrowseFilterOption<T>(
    val value: T,
    val label: String
)

@Composable
fun ComicBrowseFilterSheetContent(
    publishers: List<PublisherBrowseResult>,
    selectedPublisherId: Long?,
    onPublisherSelected: (Long?) -> Unit,
    universes: List<Universe>,
    selectedUniverseId: Long?,
    onUniverseSelected: (Long?) -> Unit,
    publicationYears: List<Int>,
    selectedPublicationYear: Int?,
    onPublicationYearSelected: (Int?) -> Unit,
    activeFilterCount: Int,
    resultCount: Int,
    onClearAll: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val publisherOptions =
        publishers.map { publisher ->
            ComicBrowseFilterOption(
                value = publisher.publisherId,
                label = publisher.name
            )
        }

    val universeOptions =
        universes.map { universe ->
            val publisherName =
                publishers.firstOrNull {
                    it.publisherId == universe.publisherId
                }?.name

            val labelParts = listOfNotNull(
                universe.designation
                    ?.takeIf { it.isNotBlank() },
                universe.name.takeIf { it.isNotBlank() },
                publisherName
            ).distinct()

            ComicBrowseFilterOption(
                value = universe.id,
                label = labelParts.joinToString(separator = " · ")
            )
        }

    val publicationYearOptions =
        publicationYears.map { year ->
            ComicBrowseFilterOption(
                value = year,
                label = year.toString()
            )
        }

    val selectedPublisherLabel =
        publisherOptions.firstOrNull {
            it.value == selectedPublisherId
        }?.label ?: "All publishers"

    val selectedUniverseLabel =
        universeOptions.firstOrNull {
            it.value == selectedUniverseId
        }?.label ?: "All universes"

    val selectedPublicationYearLabel =
        publicationYearOptions.firstOrNull {
            it.value == selectedPublicationYear
        }?.label ?: "All years"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = ComicSpacing2Xl,
                end = ComicSpacing2Xl,
                bottom = ComicSpacingXl
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(
                        color = ComicInk,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        ComicFilterTitleBanner(
            text = "FILTER BROWSE"
        )

        ComicFilterSectionLabel("PUBLISHER")

        ComicBrowseFilterSelector(
            selectedLabel = selectedPublisherLabel,
            allLabel = "All publishers",
            options = publisherOptions,
            onSelected = onPublisherSelected
        )

        ComicFilterSectionLabel("UNIVERSE / CONTINUITY")

        ComicBrowseFilterSelector(
            selectedLabel = selectedUniverseLabel,
            allLabel = "All universes",
            options = universeOptions,
            onSelected = onUniverseSelected
        )

        ComicFilterSectionLabel("PUBLICATION YEAR")

        ComicBrowseFilterSelector(
            selectedLabel = selectedPublicationYearLabel,
            allLabel = "All years",
            options = publicationYearOptions,
            onSelected = onPublicationYearSelected
        )

        if (activeFilterCount > 0) {
            ComicFilterSecondaryButton(
                text = "Clear all filters",
                onClick = onClearAll
            )
        }

        ComicFilterPrimaryButton(
            text = if (activeFilterCount > 0) {
                "Show $resultCount results"
            } else {
                "Done"
            },
            onClick = onDone
        )
    }
}

@Composable
private fun <T> ComicBrowseFilterSelector(
    selectedLabel: String,
    allLabel: String,
    options: List<ComicBrowseFilterOption<T>>,
    onSelected: (T?) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                role = Role.Button
            ) {
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(shape)
                .background(ComicPaper)
                .border(
                    width = ComicCardBorderWidth,
                    color = ComicInk,
                    shape = shape
                )
                .padding(
                    start = 14.dp,
                    end = ComicSpacingSm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedLabel,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = ComicInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Choose filter",
                tint = ComicInk,
                modifier = Modifier.size(26.dp)
            )
        }

        ComicDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            ComicDropdownMenuItem(
                text = allLabel,
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )

            options.forEach { option ->
                ComicDropdownMenuItem(
                    text = option.label,
                    onClick = {
                        onSelected(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}
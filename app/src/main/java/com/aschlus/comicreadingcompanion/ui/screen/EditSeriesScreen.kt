package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDialog
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownField
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownOption
import com.aschlus.comicreadingcompanion.ui.component.ComicFormTextField
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicIssueDetailHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicWideActionButton
import com.aschlus.comicreadingcompanion.ui.theme.ComicInk
import com.aschlus.comicreadingcompanion.ui.theme.ComicRed
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingLg
import com.aschlus.comicreadingcompanion.ui.theme.ComicSpacingMd
import com.aschlus.comicreadingcompanion.ui.theme.ComicYellow
import com.aschlus.comicreadingcompanion.ui.viewmodel.EditSeriesViewModel

@Composable
fun EditSeriesScreen(
    seriesId: Long,
    viewModel: EditSeriesViewModel,
    onBackClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val title by viewModel.title.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val startYear by viewModel.startYear.collectAsState()
    val endYear by viewModel.endYear.collectAsState()
    val publishers by viewModel.publishers.collectAsState()
    val selectedPublisherId by viewModel.selectedPublisherId.collectAsState()
    val universes by viewModel.universes.collectAsState()
    val selectedUniverseId by viewModel.selectedUniverseId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val seriesSaved by viewModel.seriesSaved.collectAsState()

    var publisherMenuExpanded by remember { mutableStateOf(false) }
    var universeMenuExpanded by remember { mutableStateOf(false) }

    var showCreatePublisherDialog by remember { mutableStateOf(false) }
    var newPublisherName by remember { mutableStateOf("") }

    var showCreateUniverseDialog by remember { mutableStateOf(false) }
    var newUniverseName by remember { mutableStateOf("") }
    var newUniverseDesignation by remember { mutableStateOf("") }
    var newUniverseDescription by remember { mutableStateOf("") }

    val selectedPublisher =
        publishers.firstOrNull {
            it.id == selectedPublisherId
        }

    val selectedUniverse =
        universes.firstOrNull {
            it.id == selectedUniverseId
        }

    LaunchedEffect(seriesId) {
        viewModel.loadSeries(seriesId)
    }

    LaunchedEffect(seriesSaved) {
        if (seriesSaved) {
            onBackClick()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ComicIssueDetailHeader(
                onBackClick = onBackClick,
                title = "EDIT SERIES"
            )
        }
    ) { innerPadding: PaddingValues ->

        if (isLoading) {
            Text(
                text = "Loading series...",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(18.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(ComicSpacingLg)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ComicHomeSectionHeader(
                    text = "SERIES DETAILS"
                )

                ComicFormTextField(
                    label = "Title",
                    value = title,
                    onValueChange = viewModel::updateTitle,
                    placeholder = "e.g. 2"
                )

                ComicFormTextField(
                    label = "Start year (optional)",
                    value = startYear,
                    onValueChange = viewModel::updateStartYear,
                    placeholder = "r.g. 1999"
                )

                ComicFormTextField(
                    label = "End year (optional)",
                    value = endYear,
                    onValueChange = viewModel::updateEndYear,
                    placeholder = "e.g. 2003"
                )

                ComicHomeSectionHeader(
                    text = "PUBLISHER & UNIVERSE"
                )

                ComicFormDropdownField(
                    text = selectedPublisher?.name
                        ?: "Select publisher",
                    expanded = publisherMenuExpanded,
                    onExpandedChange = {
                        publisherMenuExpanded = it
                    }
                ) {
                    ComicFormDropdownOption(
                        text = "CREATE NEW PUBLISHER...",
                        onClick = {
                            publisherMenuExpanded = false
                            newPublisherName = ""
                            showCreatePublisherDialog = true
                        }
                    )

                    publishers.forEach { publisher ->
                        ComicFormDropdownOption(
                            text = publisher.name,
                            onClick = {
                                viewModel.selectPublisher(
                                    publisher.id
                                )
                                publisherMenuExpanded = false
                            }
                        )
                    }
                }

                ComicFormDropdownField(
                    text = selectedUniverse?.let { universe ->
                        universe.designation?.let { designation ->
                            "${universe.name} ($designation)"
                        } ?: universe.name
                    } ?: "No universe",
                    expanded = universeMenuExpanded,
                    onExpandedChange = {
                        universeMenuExpanded = it
                    },
                    enabled = selectedPublisherId != null
                ) {
                    ComicFormDropdownOption(
                        text = "NO UNIVERSE",
                        onClick = {
                            viewModel.selectUniverse(null)
                            universeMenuExpanded = false
                        }
                    )

                    ComicFormDropdownOption(
                        text = "CREATE NEW UNIVERSE...",
                        onClick = {
                            universeMenuExpanded = false
                            newUniverseName = ""
                            newUniverseDesignation = ""
                            newUniverseDescription = ""
                            showCreateUniverseDialog = true
                        }
                    )

                    universes.forEach { universe ->
                        ComicFormDropdownOption(
                            text = universe.designation?.let { designation ->
                                "${universe.name} ($designation)"
                            } ?: universe.name,
                            onClick = {
                                viewModel.selectUniverse(universe.id)
                                universeMenuExpanded = false
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    val errorShape = RoundedCornerShape(6.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(errorShape)
                            .background(ComicRed.copy(alpha = 0.12f))
                            .border(
                                width = 2.dp,
                                color = ComicInk,
                                shape = errorShape
                            )
                            .padding(
                                horizontal = ComicSpacingMd,
                                vertical = 10.dp
                            )
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = ComicRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ComicWideActionButton(
                        text = "CANCEL",
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = onBackClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    )

                    ComicWideActionButton(
                        text = if (isSaving) "SAVING..." else "SAVE CHANGES",
                        backgroundColor = ComicYellow,
                        onClick = viewModel::saveSeries,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && selectedPublisherId != null
                    )
                }
            }
        }
    }

    if (showCreatePublisherDialog) {
        ComicFormDialog(
            title = "CREATE PUBLISHER",
            primaryLabel = "PUBLISHER NAME",
            primaryValue = newPublisherName,
            onPrimaryValueChange = {
                newPublisherName = it
            },
            secondaryLabel = "",
            secondaryValue = "",
            onSecondaryValueChange = {},
            confirmText = "CREATE",
            confirmEnabled = newPublisherName.isNotBlank(),
            onConfirm = {
                viewModel.createPublisher(newPublisherName)
                showCreatePublisherDialog = false
            },
            onDismiss = {
                showCreatePublisherDialog = false
            },
            showSecondaryField = false
        )
    }

    if (showCreateUniverseDialog) {
        ComicFormDialog(
            title = "CREATE UNIVERSE",
            primaryLabel = "UNIVERSE NAME",
            primaryValue = newUniverseName,
            onPrimaryValueChange = {
                newUniverseName = it
            },
            secondaryLabel = "CONTINUITY DESIGNATION",
            secondaryValue = newUniverseDesignation,
            onSecondaryValueChange = {
                newUniverseDesignation = it
            },
            confirmText = "CREATE",
            confirmEnabled = newUniverseName.isNotBlank(),
            onConfirm = {
                viewModel.createUniverse(
                    name = newUniverseName,
                    designation = newUniverseDesignation,
                    description = null
                )
                showCreateUniverseDialog = false
            },
            onDismiss = {
                showCreateUniverseDialog = false
            }
        )
    }
}
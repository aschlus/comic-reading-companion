package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.component.ComicAddIssuesTrigger
import com.aschlus.comicreadingcompanion.ui.component.ComicCreateReadingListHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownField
import com.aschlus.comicreadingcompanion.ui.component.ComicFormDropdownOption
import com.aschlus.comicreadingcompanion.ui.component.ComicFormTextField
import com.aschlus.comicreadingcompanion.ui.component.ComicHomeSectionHeader
import com.aschlus.comicreadingcompanion.ui.component.ComicReadingListStylePicker
import com.aschlus.comicreadingcompanion.ui.theme.ComicPaper
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.component.ComicPendingReadingListIssueRow
import com.aschlus.comicreadingcompanion.ui.component.ComicCreateReadingListActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReadingListScreen(
    viewModel: CreateReadingListViewModel,
    onBackClick: () -> Unit,
    onReadingListCreated: (Long) -> Unit
) {
    val publishers by viewModel.publishers.collectAsState()
    val universes by viewModel.universes.collectAsState()
    val title by viewModel.title.collectAsState()
    val description by viewModel.description.collectAsState()
    val selectedPublisherId by viewModel.selectedPublisherId.collectAsState()
    val selectedUniverseId by viewModel.selectedUniverseId.collectAsState()
    val canCreateReadingList by viewModel.canCreateReadingList.collectAsState()
    val createdReadingListId by viewModel.createdReadingListId.collectAsState()
    val selectedStyle by viewModel.selectedStyle.collectAsState()
    val pendingIssues by viewModel.pendingIssues.collectAsState()

    var publisherMenuExpanded by remember { mutableStateOf(false) }
    var universeMenuExpanded by remember { mutableStateOf(false) }
    var addToListMode by remember { mutableStateOf(false) }

    var selectedPublisher =
        publishers.firstOrNull { publisher ->
            publisher.id == selectedPublisherId
        }
    var selectedUniverse =
        universes.firstOrNull { universe ->
            universe.id == selectedUniverseId
        }

    LaunchedEffect(
        createdReadingListId
    ) {
        createdReadingListId?.let { readingListId ->
            onReadingListCreated(readingListId)
        }
    }

    if (addToListMode) {
        AddToReadingListScreen(
            viewModel = viewModel,
            onBackClick = {
                viewModel.cancelAddToListSession()
                addToListMode = false
            },
            onDoneClick = {
                viewModel.applyAddToListSession()
                addToListMode = false
            }
        )

        return
    }

    Scaffold(
        containerColor = ComicPaper,
        topBar = {
            ComicCreateReadingListHeader(
                onBackClick = onBackClick
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ComicHomeSectionHeader(
                text = "LIST DETAILS"
            )

            ComicFormTextField(
                label = "Title",
                value = title,
                onValueChange =
                    viewModel::updateTitle,
                placeholder = "Reading list title"
            )

            ComicFormTextField(
                label = "Description (optional)",
                value = description,
                onValueChange =
                    viewModel::updateDescription,
                placeholder = "Add a short description",
                singleLine = false,
                minHeight = 96
            )

            ComicHomeSectionHeader(
                text = "LIST STYLE"
            )

            ComicReadingListStylePicker(
                selectedStyle = selectedStyle,
                onStyleSeclected =
                    viewModel::selectStyle
            )

            ComicHomeSectionHeader(
                text = "PUBLISHER & CONTINUITY"
            )

            ComicFormDropdownField(
                text = selectedPublisher
                    ?.name
                    ?: "Select publisher",
                expanded = publisherMenuExpanded,
                onExpandedChange = {
                    publisherMenuExpanded = it
                }
            ) {
                publishers.forEach { publisher ->
                    ComicFormDropdownOption(
                        text = publisher.name,
                        onClick = {
                            viewModel.selectPublisher(publisher.id)

                            publisherMenuExpanded = false
                        }
                    )
                }
            }

            ComicFormDropdownField(
                text = selectedUniverse
                    ?.name
                    ?: "No specific continuity",
                expanded = universeMenuExpanded,
                onExpandedChange = {
                    universeMenuExpanded = it
                },
                enabled = selectedPublisherId != null
            ) {
                ComicFormDropdownOption(
                    text = "No specific continuity",
                    onClick = {
                        viewModel.selectUniverse(null)

                        universeMenuExpanded = false
                    }
                )

                universes.forEach { universe ->
                    ComicFormDropdownOption(
                        text = universe.name,
                        onClick = {
                            viewModel.selectUniverse(universe.id)

                            universeMenuExpanded = false
                        }
                    )
                }
            }

            ComicHomeSectionHeader(
                text = "ADD ISSUES OR SERIES"
            )

            ComicAddIssuesTrigger(
                onClick = {
                    viewModel.beginAddToListSession()
                    addToListMode = true
                }
            )

            if(pendingIssues.isNotEmpty()) {
                ComicHomeSectionHeader(
                    text = "ADDED TO LIST"
                )

                pendingIssues.forEachIndexed { index, issue ->
                    ComicPendingReadingListIssueRow(
                        issue = issue,
                        canMoveUp = index > 0,
                        canMoveDown = index < pendingIssues.lastIndex,
                        onMoveUp = {
                            viewModel.movePendingIssue(
                                issueId = issue.issueId,
                                offset = -1
                            )
                        },
                        onMoveDown = {
                            viewModel.movePendingIssue(
                                issueId = issue.issueId,
                                offset = 1
                            )
                        },
                        onRemove = {
                            viewModel.removePendingIssue(issue.issueId)
                        }
                    )
                }
            }

            ComicCreateReadingListActions(
                canSave = canCreateReadingList,
                onCancelClick = onBackClick,
                onSaveClick = {
                    viewModel.createReadingList()
                }
            )
        }
    }
}
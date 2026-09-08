package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import kotlin.OptIn

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

    var publisherMenuExpanded by remember { mutableStateOf(false) }
    var universeMenuExpanded by remember { mutableStateOf(false) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Create Reading List")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = {Text("Title")},
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = viewModel::updateDescription,
                modifier = Modifier.fillMaxWidth(),
                label = {Text("Description (optional)")},
                minLines = 3
            )

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        publisherMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedPublisher
                            ?.name
                            ?: "Select publisher"
                    )
                }

                DropdownMenu(
                    expanded = publisherMenuExpanded,
                    onDismissRequest = {
                        publisherMenuExpanded = false
                    }
                ) {
                    publishers.forEach { publisher ->
                        DropdownMenuItem(
                            text = {
                                Text(publisher.name)
                            },
                            onClick = {
                                viewModel.selectPublisher(publisher.id)
                                publisherMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        universeMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedPublisherId != null
                ) {
                    Text(
                        selectedUniverse
                            ?.name
                            ?: "No specific continuity"
                    )
                }

                DropdownMenu(
                    expanded = universeMenuExpanded,
                    onDismissRequest = {
                        universeMenuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("No specific continuity")
                        },
                        onClick = {
                            viewModel.selectUniverse(null)
                            universeMenuExpanded = false
                        }
                    )

                    universes.forEach { universe ->
                        DropdownMenuItem(
                            text = {
                                Text(universe.name)
                            },
                            onClick = {
                                viewModel.selectUniverse(universe.id)
                                universeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.createReadingList()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canCreateReadingList
            ) {
                Text("Create Reading List")
            }
        }
    }
}
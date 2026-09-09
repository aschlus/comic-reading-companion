package com.aschlus.comicreadingcompanion.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIssueToReadingListSheet(
    viewModel: AddIssueToReadingListViewModel,
    onDismissRequest: () -> Unit
) {
    val readingLists by viewModel.userReadingLists.collectAsState()
    val isAdding by viewModel.isAdding.collectAsState()
    val addedReadingListId by viewModel.addedReadingListId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    LaunchedEffect(
        addedReadingListId
    ) {
        if (addedReadingListId != null) {
            viewModel.clearResult()
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearResult()
            onDismissRequest()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add to Reading List",
                style = MaterialTheme.typography.headlineSmall
            )

            if (isAdding) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (readingLists.isEmpty()) {
                Text(
                    text = "No custom reading lists yet.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 400.dp
                        )
                ) {
                    items(
                        items = readingLists,
                        key = { readingList -> readingList.id}
                    ) { readingList ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    enabled = !isAdding
                                ) {
                                    viewModel
                                        .addToReadingList(
                                            readingList.id
                                        )
                                }
                                .padding(
                                    vertical = 12.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = readingList.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            readingList.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
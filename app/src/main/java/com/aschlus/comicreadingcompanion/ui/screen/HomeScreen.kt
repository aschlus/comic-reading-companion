package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.data.database.entities.ReadingList
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onReadingListClick: (Long) -> Unit
) {
    val readingLists by viewModel.readingLists.collectAsState()

    val readingListSummaries by
        viewModel.readingListSummaries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Comic Reading Companion")
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("My Reading Lists")

            if (readingLists.isEmpty()) {
                Text("No reading lists yet")
            } else {
                readingLists.forEach { readingList ->
                    val summary = readingListSummaries.firstOrNull {
                        it.readingListId == readingList.id
                    }

                    ReadingListCard(
                        readingList = readingList,
                        readCount = summary?.readCount ?: 0,
                        totalCount = summary?.totalCount ?: 0,
                        onClick = {
                            onReadingListClick(readingList.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingListCard(
    readingList: ReadingList,
    readCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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

            val progress =
                if (totalCount == 0) {
                    0f
                } else {
                    readCount.toFloat() / totalCount.toFloat()
                }

            val completionPercentage =
                if (totalCount == 0) {
                    0f
                } else {
                    (readCount * 100) / totalCount
                }

            Text(
                text = "$readCount of $totalCount read • " +
                    "$completionPercentage% complete",
                style = MaterialTheme.typography.bodySmall
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
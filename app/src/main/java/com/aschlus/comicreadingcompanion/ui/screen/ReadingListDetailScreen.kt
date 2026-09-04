package com.aschlus.comicreadingcompanion.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListDetailScreen(
    readingListId: Long,
    viewModel: ReadingListDetailViewModel
) {
    val readingList by
        viewModel.readingList.collectAsState()

    LaunchedEffect(readingListId) {
        viewModel.loadReadingList(readingListId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        readingList?.title
                            ?: "Reading List"
                    )
                }
            )
        }
    ) { innerPadding: PaddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            val currentReadingList = readingList

            if (currentReadingList == null) {
                Text("Loading...")
            } else {
                currentReadingList.description
                    ?.let { description ->
                        Text(description)
                    }
            }
        }
    }
}
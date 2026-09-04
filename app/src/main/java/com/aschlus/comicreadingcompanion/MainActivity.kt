package com.aschlus.comicreadingcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            (application as ComicReadingCompanionApplication)
                .container
                .comicRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComicReadingCompanionTheme {

                val publishers by homeViewModel.publishers.collectAsState()

                LaunchedEffect(Unit) {
                    homeViewModel.loadPublishers()
                }

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Text("Comic Reading Companion")

                        if (publishers.isEmpty()) {
                            Text("No publishers yet")
                        } else {
                            publishers.forEach { publisher ->
                                Text(publisher.name)
                            }
                        }
                    }
                }

            }
        }
    }
}
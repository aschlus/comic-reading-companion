package com.aschlus.comicreadingcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aschlus.comicreadingcompanion.ui.screen.HomeScreen
import com.aschlus.comicreadingcompanion.ui.screen.ReadingListDetailScreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModelFactory

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

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onReadingListClick = { readingListId ->
                                navController.navigate(
                                    "readingList/$readingListId"
                                )
                            }
                        )
                    }

                    composable(
                        route = "readingList/{readingListId}",
                        arguments = listOf(
                            navArgument("readingListId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->

                        val readingListId =
                            backStackEntry.arguments
                                ?.getLong("readingListId")
                                ?: return@composable

                        val detailViewModel:
                                ReadingListDetailViewModel = viewModel(
                                    factory =
                                        ReadingListDetailViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )

                        ReadingListDetailScreen(
                            readingListId = readingListId,
                            viewModel = detailViewModel
                        )
                    }
                }
            }
        }
    }
}
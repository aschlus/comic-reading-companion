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
import com.aschlus.comicreadingcompanion.ui.screen.IssueDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.ReadingListDetailScreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModelFactory
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
                            onReadingListClick = { readingListId, startPosition ->
                                navController.navigate(
                                    "readingList/$readingListId?startPosition=$startPosition"
                                )
                            }
                        )
                    }

                    composable(
                        route = "readingList/{readingListId}?startPosition={startPosition}",
                        arguments = listOf(
                            navArgument("readingListId") {
                                type = NavType.LongType
                            },
                            navArgument("startPosition") {
                                type = NavType.IntType
                                defaultValue = -1
                            }
                        )
                    ) { backStackEntry ->

                        val readingListId =
                            backStackEntry.arguments
                                ?.getLong("readingListId")
                                ?: return@composable

                        val startPosition =
                            backStackEntry.arguments
                                ?.getInt("startPosition")
                                ?: -1

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
                            startPosition = startPosition,
                            viewModel = detailViewModel,
                            onIssueClick = { issueId ->
                                navController.navigate(
                                    "issue/$issueId"
                                )
                            },
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(
                        route = "issue/{issueId}",
                        arguments = listOf(
                            navArgument("issueId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->

                        val issueId =
                            backStackEntry.arguments
                                ?.getLong("issueId")
                                ?: return@composable

                        val issueDetailViewModel:
                                IssueDetailViewModel = viewModel(
                                    factory =
                                        IssueDetailViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )

                        IssueDetailScreen(
                            issueId = issueId,
                            viewModel = issueDetailViewModel,
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
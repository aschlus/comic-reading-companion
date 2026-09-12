package com.aschlus.comicreadingcompanion

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aschlus.comicreadingcompanion.data.exporter.readingListExportFileName
import com.aschlus.comicreadingcompanion.ui.screen.BrowseScreen
import com.aschlus.comicreadingcompanion.ui.screen.CreateReadingListScreen
import com.aschlus.comicreadingcompanion.ui.screen.HomeScreen
import com.aschlus.comicreadingcompanion.ui.screen.IssueDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.PublisherDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.ReadingListDetailScreen
import com.aschlus.comicreadingcompanion.ui.screen.SeriesDetailScreen
import com.aschlus.comicreadingcompanion.ui.theme.ComicReadingCompanionTheme
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.AddIssueToReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.BrowseViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.CreateReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.HomeViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ImportReadingListViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.IssueDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.PublisherDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.ReadingListDetailViewModelFactory
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModel
import com.aschlus.comicreadingcompanion.ui.viewmodel.SeriesDetailViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            (application as ComicReadingCompanionApplication)
                .container
                .comicRepository
        )
    }

    private val importReadingListViewModel:
            ImportReadingListViewModel by viewModels {
                val container =
                    (application as ComicReadingCompanionApplication)
                        .container

        ImportReadingListViewModelFactory(
            parser = container.readingListAssetParser,
            importer = container.readingListImporter
        )
    }

    private val importReadingListLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val fileName =
                        getDisplayName(uri)
                            ?: "selected file"

                    val jsonText =
                        withContext(
                            Dispatchers.IO
                        ) {
                            contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { reader ->
                                    reader.readText()
                                }
                                ?: throw IllegalArgumentException(
                                    "Could not read selected file '$fileName"
                                )
                        }

                    importReadingListViewModel
                        .importReadingList(
                            jsonText = jsonText,
                            sourceDescription = "reading-list file '$fileName'"
                        )
                } catch (
                    exception: Exception
                ) {
                    importReadingListViewModel
                        .reportError(
                            exception.message
                                ?: "Could not read selected reading-list file"
                        )
                }
            }
        }

    private var pendingExportReadingListId:
            Long? = null

    private val exportReadingListLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->
            val readingListId =
                pendingExportReadingListId

            pendingExportReadingListId = null

            if (uri == null || readingListId == null) {
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                try {
                    val exporter =
                        (application as ComicReadingCompanionApplication)
                            .container
                            .readingListExporter

                    val jsonText =
                        withContext(
                            Dispatchers.IO
                        ) {
                            exporter.exportJson(
                                readingListId = readingListId
                            )
                        }

                    withContext(
                        Dispatchers.IO
                    ) {
                        contentResolver
                            .openOutputStream(uri, "wt")
                            ?.bufferedWriter()
                            ?.use { writer ->
                                writer.write(jsonText)
                            }
                            ?: throw IllegalArgumentException(
                                "Could not write exported file"
                            )
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Reading list exported",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (
                    exception : Exception
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        exception.message
                            ?: "Could not export reading list",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private fun getDisplayName(
        uri: Uri
    ): String? {
        contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->
                val columnIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (columnIndex >= 0 && cursor.moveToFirst()) {
                    return cursor.getString(columnIndex)
                }
            }

        return uri.lastPathSegment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComicReadingCompanionTheme {

                val navController = rememberNavController()

                val safeNavigateBack: () -> Unit = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        val importState by importReadingListViewModel.state.collectAsState()

                        HomeScreen(
                            viewModel = homeViewModel,
                            onBrowseClick = {
                                navController.navigate("browse")
                            },
                            onCreateReadingListClick = {
                                navController.navigate(
                                    "createReadingList"
                                )
                            },
                            onImportReadingListClick = {
                                importReadingListLauncher.launch(
                                    arrayOf("application/json", "text/plain")
                                )
                            },
                            importState = importState,
                            onImportResultConsumed = {
                                importReadingListViewModel.clearResult()
                            },
                            onReadingListClick = { readingListId, startPosition ->
                                navController.navigate(
                                    "readingList/$readingListId?startPosition=$startPosition"
                                )
                            }
                        )
                    }

                    composable("createReadingList") {
                        val createReadingListViewModel: CreateReadingListViewModel =
                            viewModel(
                                factory =
                                    CreateReadingListViewModelFactory(
                                        (application as ComicReadingCompanionApplication)
                                            .container
                                            .comicRepository
                                    )
                            )
                        CreateReadingListScreen(
                            viewModel = createReadingListViewModel,
                            onBackClick = safeNavigateBack,
                            onReadingListCreated = { readingListId ->
                                navController.navigate(
                                    "readingList/$readingListId?startPosition=-1"
                                ) {
                                    popUpTo("createReadingList") {
                                        inclusive = true
                                    }
                                }
                            }
                        )
                    }

                    composable("browse") {
                        val browseViewModel:
                                BrowseViewModel = viewModel(
                                    factory =
                                        BrowseViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )

                        BrowseScreen(
                            viewModel = browseViewModel,
                            onPublisherClick = { publisherId ->
                                navController.navigate(
                                    "publisher/$publisherId"
                                )
                            },
                            onSeriesClick = { seriesId ->
                                navController.navigate(
                                    "series/$seriesId"
                                )
                            },
                            onIssueClick = { issueId ->
                                navController.navigate(
                                    "issue/$issueId"
                                )
                            },
                            onBackClick = safeNavigateBack
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
                                            repository =
                                                (application as ComicReadingCompanionApplication)
                                                    .container
                                                    .comicRepository,
                                            readingListUiPreferences =
                                                (application as ComicReadingCompanionApplication)
                                                    .container
                                                    .readingListUiPreferences
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
                            onBackClick = safeNavigateBack,
                            onExportReadingListClick = { id, title ->
                                pendingExportReadingListId = id

                                exportReadingListLauncher.launch(
                                    readingListExportFileName(title)
                                )
                            }
                        )
                    }

                    composable(
                        route = "publisher/{publisherId}",
                        arguments = listOf(
                            navArgument("publisherId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->

                        val publisherId =
                            backStackEntry.arguments
                                ?.getLong("publisherId")
                                ?: return@composable

                        val publisherDetailViewModel:
                                PublisherDetailViewModel = viewModel(
                                    factory =
                                        PublisherDetailViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )

                        PublisherDetailScreen(
                            publisherId = publisherId,
                            viewModel = publisherDetailViewModel,
                            onSeriesClick = { seriesId ->
                                navController.navigate(
                                    "series/$seriesId"
                                )
                            },
                            onBackClick = safeNavigateBack
                        )
                    }

                    composable(
                        route = "series/{seriesId}",
                        arguments = listOf(
                            navArgument("seriesId") {
                                type = NavType.LongType
                            }
                        )
                    ) { backStackEntry ->

                        val seriesId =
                            backStackEntry.arguments
                                ?.getLong("seriesId")
                                ?: return@composable

                        val seriesDetailViewModel:
                                SeriesDetailViewModel = viewModel(
                                    factory =
                                        SeriesDetailViewModelFactory(
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                        )
                                )

                        SeriesDetailScreen(
                            seriesId = seriesId,
                            viewModel = seriesDetailViewModel,
                            onPublisherClick = { publisherId ->
                                navController.navigate(
                                    "publisher/$publisherId"
                                )
                            },
                            onIssueClick = { issueId ->
                                navController.navigate(
                                    "issue/$issueId"
                                )
                            },
                            onBackClick = safeNavigateBack
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

                        val addIssueToReadingListViewModel:
                                AddIssueToReadingListViewModel =
                            viewModel(
                                factory =
                                    AddIssueToReadingListViewModelFactory(
                                        issueId = issueId,
                                        repository =
                                            (application as ComicReadingCompanionApplication)
                                                .container
                                                .comicRepository
                                    )
                            )

                        IssueDetailScreen(
                            issueId = issueId,
                            viewModel = issueDetailViewModel,
                            addToReadingListViewModel =
                                addIssueToReadingListViewModel,
                            onSeriesClick = {seriesId ->
                                navController.navigate(
                                    "series/$seriesId"
                                )
                            },
                            onBackClick = safeNavigateBack
                        )
                    }
                }
            }
        }
    }
}